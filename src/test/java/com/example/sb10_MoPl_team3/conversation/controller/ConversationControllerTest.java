package com.example.sb10_MoPl_team3.conversation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.sb10_MoPl_team3.conversation.dto.request.ConversationCreateRequest;
import com.example.sb10_MoPl_team3.conversation.dto.request.ConversationFindAllRequest;
import com.example.sb10_MoPl_team3.conversation.dto.response.ConversationDto;
import com.example.sb10_MoPl_team3.conversation.dto.response.CursorResponseConversationDto;
import com.example.sb10_MoPl_team3.conversation.service.ConversationService;
import com.example.sb10_MoPl_team3.global.config.SecurityConfig;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.global.exception.GlobalExceptionHandler;
import com.example.sb10_MoPl_team3.global.security.AuthUser;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtProvider;
import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ConversationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConversationService conversationService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("대화방 목록 조회 쿼리 파라미터를 요청 DTO로 바인딩한다")
    void findConversations_bindsQueryParameters() throws Exception {
        UUID requestUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID idAfter = UUID.fromString("00000000-0000-0000-0000-000000000099");
        CursorResponseConversationDto<ConversationDto> response =
            new CursorResponseConversationDto<>(
                List.of(),
                null,
                null,
                false,
                0L,
                "createdAt",
                "DESCENDING"
            );

        given(conversationService.findAll(eq(requestUserId), any(ConversationFindAllRequest.class)))
            .willReturn(response);

        mockMvc.perform(get("/api/conversations")
                .with(authentication(authToken(requestUserId)))
                .param("keywordLike", "상대")
                .param("cursor", "2026-06-29T00:00:00Z")
                .param("idAfter", idAfter.toString())
                .param("limit", "20")
                .param("sortDirection", "DESCENDING")
                .param("sortBy", "createdAt"))
            .andExpect(status().isOk());

        ArgumentCaptor<ConversationFindAllRequest> captor =
            ArgumentCaptor.forClass(ConversationFindAllRequest.class);
        then(conversationService).should().findAll(eq(requestUserId), captor.capture());

        ConversationFindAllRequest capturedRequest = captor.getValue();
        assertThat(capturedRequest.keywordLike()).isEqualTo("상대");
        assertThat(capturedRequest.cursor()).isEqualTo("2026-06-29T00:00:00Z");
        assertThat(capturedRequest.idAfter()).isEqualTo(idAfter);
        assertThat(capturedRequest.limit()).isEqualTo(20);
        assertThat(capturedRequest.sortDirection()).isEqualTo("DESCENDING");
        assertThat(capturedRequest.sortBy()).isEqualTo("createdAt");
    }

    @Test
    @DisplayName("대화방 목록 조회 요청을 쿼리 파라미터로 바인딩하고 200을 반환한다")
    void findConversations_success() throws Exception {
        UUID requestUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID conversationId = UUID.fromString("00000000-0000-0000-0000-000000000011");
        UUID withUserId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        ConversationDto conversationDto = new ConversationDto(
            conversationId,
            new UserSummary(withUserId, "상대", null),
            null,
            false
        );
        CursorResponseConversationDto<ConversationDto> response =
            new CursorResponseConversationDto<>(
                List.of(conversationDto),
                null,
                null,
                false,
                1L,
                "createdAt",
                "DESCENDING"
            );

        given(conversationService.findAll(eq(requestUserId), any(ConversationFindAllRequest.class)))
            .willReturn(response);

        mockMvc.perform(get("/api/conversations")
                .with(authentication(authToken(requestUserId)))
                .param("keywordLike", "상대")
                .param("limit", "20")
                .param("sortDirection", "DESCENDING")
                .param("sortBy", "createdAt"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].id").value(conversationId.toString()))
            .andExpect(jsonPath("$.data[0].with.userId").value(withUserId.toString()))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.totalCount").value(1))
            .andExpect(jsonPath("$.sortBy").value("createdAt"))
            .andExpect(jsonPath("$.sortDirection").value("DESCENDING"));
    }

    @Test
    @DisplayName("대화방 생성 요청이 유효하면 ConversationDto를 200으로 반환한다")
    void createConversation_success() throws Exception {
        UUID requestUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID conversationId = UUID.fromString("00000000-0000-0000-0000-000000000011");
        UUID withUserId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        ConversationDto response = new ConversationDto(
            conversationId,
            new UserSummary(withUserId, "상대", null),
            null,
            false
        );

        given(conversationService.create(eq(requestUserId), any(ConversationCreateRequest.class)))
            .willReturn(response);

        mockMvc.perform(post("/api/conversations")
                .with(authentication(authToken(requestUserId)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "withUserId": "00000000-0000-0000-0000-000000000002"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(conversationId.toString()))
            .andExpect(jsonPath("$.with.userId").value(withUserId.toString()))
            .andExpect(jsonPath("$.hasUnread").value(false));
    }

    @Test
    @DisplayName("대화방 생성 요청에 상대 사용자 ID가 없으면 400을 반환한다")
    void createConversation_missingWithUserId() throws Exception {
        UUID requestUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockMvc.perform(post("/api/conversations")
                .with(authentication(authToken(requestUserId)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    @DisplayName("대화방 ID 단건 조회 요청이 유효하면 ConversationDto를 반환한다")
    void findConversation_success() throws Exception {
        UUID requestUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID conversationId = UUID.fromString("00000000-0000-0000-0000-000000000011");
        UUID withUserId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        ConversationDto response = new ConversationDto(
            conversationId,
            new UserSummary(withUserId, "상대", null),
            null,
            false
        );

        given(conversationService.find(requestUserId, conversationId)).willReturn(response);

        mockMvc.perform(get("/api/conversations/{conversationId}", conversationId)
                .with(authentication(authToken(requestUserId))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(conversationId.toString()))
            .andExpect(jsonPath("$.with.name").value("상대"));
    }

    @Test
    @DisplayName("대화방 ID 단건 조회 대상이 없으면 404를 반환한다")
    void findConversation_notFound() throws Exception {
        UUID requestUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID conversationId = UUID.fromString("00000000-0000-0000-0000-000000000011");

        given(conversationService.find(requestUserId, conversationId))
            .willThrow(new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));

        mockMvc.perform(get("/api/conversations/{conversationId}", conversationId)
                .with(authentication(authToken(requestUserId))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("CONVERSATION_NOT_FOUND"));
    }

    @Test
    @DisplayName("대화방 목록 조회 파라미터가 잘못되면 400을 반환한다")
    void findConversations_invalidRequest() throws Exception {
        UUID requestUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        given(conversationService.findAll(eq(requestUserId), any(ConversationFindAllRequest.class)))
            .willThrow(new BusinessException(ErrorCode.INVALID_CURSOR));

        mockMvc.perform(get("/api/conversations")
                .with(authentication(authToken(requestUserId)))
                .param("cursor", "invalid-cursor")
                .param("limit", "20")
                .param("sortDirection", "DESCENDING")
                .param("sortBy", "createdAt"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_CURSOR"));
    }

    @Test
    @DisplayName("명세상 필수 쿼리 파라미터가 없으면 대화방 목록 조회는 400을 반환한다")
    void findConversations_missingRequiredParameter() throws Exception {
        UUID requestUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        mockMvc.perform(get("/api/conversations")
                .with(authentication(authToken(requestUserId)))
                .param("sortDirection", "DESCENDING")
                .param("sortBy", "createdAt"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
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
