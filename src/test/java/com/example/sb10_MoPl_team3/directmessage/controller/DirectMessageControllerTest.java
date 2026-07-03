package com.example.sb10_MoPl_team3.directmessage.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.sb10_MoPl_team3.directmessage.dto.DirectMessageDto;
import com.example.sb10_MoPl_team3.directmessage.dto.response.CursorResponseDirectMessageDto;
import com.example.sb10_MoPl_team3.directmessage.service.DirectMessageService;
import com.example.sb10_MoPl_team3.global.config.SecurityConfig;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.global.exception.GlobalExceptionHandler;
import com.example.sb10_MoPl_team3.global.security.AuthUser;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtProvider;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtSessionValidator;
import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DirectMessageController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class DirectMessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DirectMessageService directMessageService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private JwtSessionValidator jwtSessionValidator;

    @Test
    @DisplayName("과거 쪽지 목록 조회 요청이 유효하면 커서 응답을 반환한다")
    void findDirectMessages_success() throws Exception {
        UUID requestUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID conversationId = UUID.fromString("00000000-0000-0000-0000-000000000011");
        UUID messageId = UUID.fromString("00000000-0000-0000-0000-000000000101");
        DirectMessageDto directMessageDto = new DirectMessageDto(
            messageId,
            conversationId,
            Instant.parse("2026-06-29T00:00:00Z"),
            new UserSummary(requestUserId, "발신자", null),
            new UserSummary(UUID.fromString("00000000-0000-0000-0000-000000000002"), "수신자", null),
            "안녕하세요"
        );
        CursorResponseDirectMessageDto<DirectMessageDto> response =
            new CursorResponseDirectMessageDto<>(
                List.of(directMessageDto),
                null,
                null,
                false,
                1L,
                "createdAt",
                "ASCENDING"
            );

        given(directMessageService.findAll(
            eq(requestUserId),
            eq(conversationId),
            any(),
            any(),
            eq(20),
            eq("ASCENDING"),
            eq("createdAt")
        )).willReturn(response);

        mockMvc.perform(get("/api/conversations/{conversationId}/direct-messages", conversationId)
                .with(authentication(authToken(requestUserId)))
                .param("limit", "20")
                .param("sortDirection", "ASCENDING")
                .param("sortBy", "createdAt"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].id").value(messageId.toString()))
            .andExpect(jsonPath("$.data[0].conversationId").value(conversationId.toString()))
            .andExpect(jsonPath("$.data[0].content").value("안녕하세요"))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    @DisplayName("대화방 소속원이 아닌 사용자가 쪽지 목록을 조회하면 403을 반환한다")
    void findDirectMessages_forbidden() throws Exception {
        UUID requestUserId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        UUID conversationId = UUID.fromString("00000000-0000-0000-0000-000000000011");

        given(directMessageService.findAll(
            eq(requestUserId),
            eq(conversationId),
            any(),
            any(),
            eq(20),
            eq("DESCENDING"),
            eq("createdAt")
        )).willThrow(new BusinessException(ErrorCode.ACCESS_DENIED));

        mockMvc.perform(get("/api/conversations/{conversationId}/direct-messages", conversationId)
                .with(authentication(authToken(requestUserId)))
                .param("limit", "20")
                .param("sortDirection", "DESCENDING")
                .param("sortBy", "createdAt"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("대화방이 없으면 쪽지 목록 조회는 404를 반환한다")
    void findDirectMessages_conversationNotFound() throws Exception {
        UUID requestUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID conversationId = UUID.fromString("00000000-0000-0000-0000-000000000011");

        given(directMessageService.findAll(
            eq(requestUserId),
            eq(conversationId),
            any(),
            any(),
            eq(20),
            eq("DESCENDING"),
            eq("createdAt")
        )).willThrow(new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));

        mockMvc.perform(get("/api/conversations/{conversationId}/direct-messages", conversationId)
                .with(authentication(authToken(requestUserId)))
                .param("limit", "20")
                .param("sortDirection", "DESCENDING")
                .param("sortBy", "createdAt"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("CONVERSATION_NOT_FOUND"));
    }

    @Test
    @DisplayName("대화방 ID 형식이 올바르지 않으면 쪽지 목록 조회는 400을 반환한다")
    void findDirectMessages_invalidConversationId() throws Exception {
        UUID requestUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockMvc.perform(get("/api/conversations/{conversationId}/direct-messages", "invalid-id")
                .with(authentication(authToken(requestUserId)))
                .param("limit", "20")
                .param("sortDirection", "DESCENDING")
                .param("sortBy", "createdAt"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_TYPE"));
    }

    @Test
    @DisplayName("쪽지 목록 조회 limit이 0이면 400을 반환한다")
    void findDirectMessages_invalidLimit() throws Exception {
        UUID requestUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID conversationId = UUID.fromString("00000000-0000-0000-0000-000000000011");

        given(directMessageService.findAll(
            eq(requestUserId),
            eq(conversationId),
            any(),
            any(),
            eq(0),
            eq("DESCENDING"),
            eq("createdAt")
        )).willThrow(new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

        mockMvc.perform(get("/api/conversations/{conversationId}/direct-messages", conversationId)
                .with(authentication(authToken(requestUserId)))
                .param("limit", "0")
                .param("sortDirection", "DESCENDING")
                .param("sortBy", "createdAt"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    @DisplayName("명세상 필수 쿼리 파라미터가 없으면 쪽지 목록 조회는 400을 반환한다")
    void findDirectMessages_missingRequiredParameter() throws Exception {
        UUID requestUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID conversationId = UUID.fromString("00000000-0000-0000-0000-000000000011");

        mockMvc.perform(get("/api/conversations/{conversationId}/direct-messages", conversationId)
                .with(authentication(authToken(requestUserId)))
                .param("sortDirection", "DESCENDING")
                .param("sortBy", "createdAt"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DM 읽음 처리 요청이 유효하면 200을 반환한다")
    void read_success() throws Exception {
        UUID requestUserId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID directMessageId = UUID.randomUUID();

        mockMvc.perform(post(
                        "/api/conversations/{conversationId}/direct-messages/{directMessageId}/read",
                        conversationId, directMessageId)
                        .with(csrf())
                        .with(authentication(authToken(requestUserId))))
                .andExpect(status().isOk());

        then(directMessageService).should()
                .read(requestUserId, conversationId, directMessageId);
    }

    @Test
    @DisplayName("DM 읽음 처리 권한이 없으면 403을 반환한다")
    void read_forbidden() throws Exception {
        UUID requestUserId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID directMessageId = UUID.randomUUID();
        willThrow(new BusinessException(ErrorCode.ACCESS_DENIED))
                .given(directMessageService)
                .read(requestUserId, conversationId, directMessageId);

        mockMvc.perform(post(
                        "/api/conversations/{conversationId}/direct-messages/{directMessageId}/read",
                        conversationId, directMessageId)
                        .with(csrf())
                        .with(authentication(authToken(requestUserId))))
                .andExpect(status().isForbidden());
    }

    private UsernamePasswordAuthenticationToken authToken(UUID userId) {
        AuthUser authUser = new AuthUser(userId, UserRole.USER, UUID.randomUUID());
        return new UsernamePasswordAuthenticationToken(
            authUser,
            null,
            authUser.authorities()
        );
    }
}
