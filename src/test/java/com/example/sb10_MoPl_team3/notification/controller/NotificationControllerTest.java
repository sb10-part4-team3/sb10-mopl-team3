package com.example.sb10_MoPl_team3.notification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.sb10_MoPl_team3.global.config.SecurityConfig;
import com.example.sb10_MoPl_team3.global.exception.GlobalExceptionHandler;
import com.example.sb10_MoPl_team3.global.security.AuthUser;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtProvider;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtSessionValidator;
import com.example.sb10_MoPl_team3.notification.dto.CursorResponseNotificationDto;
import com.example.sb10_MoPl_team3.notification.dto.NotificationDto;
import com.example.sb10_MoPl_team3.notification.dto.NotificationFindAllRequest;
import com.example.sb10_MoPl_team3.notification.enums.NotificationLevel;
import com.example.sb10_MoPl_team3.notification.service.NotificationService;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private JwtSessionValidator jwtSessionValidator;

    @Test
    @DisplayName("알림 목록 조회 쿼리 파라미터를 요청 DTO로 바인딩한다")
    void findNotifications_bindsQueryParameters() throws Exception {
        UUID receiverId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID idAfter = UUID.fromString("00000000-0000-0000-0000-000000000099");
        CursorResponseNotificationDto<NotificationDto> response =
                new CursorResponseNotificationDto<>(
                        List.of(),
                        null,
                        null,
                        false,
                        0L,
                        "createdAt",
                        "DESCENDING");
        given(notificationService.findAll(eq(receiverId), any(NotificationFindAllRequest.class)))
                .willReturn(response);

        mockMvc.perform(get("/api/notifications")
                        .with(authentication(authToken(receiverId)))
                        .param("cursor", "2026-06-29T00:00:00Z")
                        .param("idAfter", idAfter.toString())
                        .param("limit", "20")
                        .param("sortDirection", "DESCENDING")
                        .param("sortBy", "createdAt"))
                .andExpect(status().isOk());

        ArgumentCaptor<NotificationFindAllRequest> captor =
                ArgumentCaptor.forClass(NotificationFindAllRequest.class);
        then(notificationService).should().findAll(eq(receiverId), captor.capture());

        NotificationFindAllRequest capturedRequest = captor.getValue();
        Assertions.assertThat(capturedRequest.cursor()).isEqualTo("2026-06-29T00:00:00Z");
        Assertions.assertThat(capturedRequest.idAfter()).isEqualTo(idAfter);
        Assertions.assertThat(capturedRequest.limit()).isEqualTo(20);
        Assertions.assertThat(capturedRequest.sortDirection()).isEqualTo("DESCENDING");
        Assertions.assertThat(capturedRequest.sortBy()).isEqualTo("createdAt");
    }

    @Test
    @DisplayName("알림 목록 조회 요청이 성공하면 커서 응답을 반환한다")
    void findNotifications_success() throws Exception {
        UUID receiverId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID notificationId = UUID.fromString("00000000-0000-0000-0000-000000000011");
        NotificationDto notificationDto = new NotificationDto(
                notificationId,
                Instant.parse("2026-06-29T00:00:00Z"),
                receiverId,
                "제목",
                "내용",
                NotificationLevel.INFO);
        CursorResponseNotificationDto<NotificationDto> response =
                new CursorResponseNotificationDto<>(
                        List.of(notificationDto),
                        null,
                        null,
                        false,
                        1L,
                        "createdAt",
                        "DESCENDING");
        given(notificationService.findAll(eq(receiverId), any(NotificationFindAllRequest.class)))
                .willReturn(response);

        mockMvc.perform(get("/api/notifications")
                        .with(authentication(authToken(receiverId)))
                        .param("limit", "20")
                        .param("sortDirection", "DESCENDING")
                        .param("sortBy", "createdAt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(notificationId.toString()))
                .andExpect(jsonPath("$.data[0].receiverId").value(receiverId.toString()))
                .andExpect(jsonPath("$.data[0].title").value("제목"))
                .andExpect(jsonPath("$.data[0].content").value("내용"))
                .andExpect(jsonPath("$.data[0].level").value("INFO"))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.sortBy").value("createdAt"))
                .andExpect(jsonPath("$.sortDirection").value("DESCENDING"));
    }

    @Test
    @DisplayName("알림 읽음 처리 요청이 성공하면 204를 반환한다")
    void read_success() throws Exception {
        UUID receiverId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();

        mockMvc.perform(delete("/api/notifications/{notificationId}", notificationId)
                        .with(csrf())
                        .with(authentication(authToken(receiverId))))
                .andExpect(status().isNoContent());

        then(notificationService).should().read(receiverId, notificationId);
    }

    private UsernamePasswordAuthenticationToken authToken(UUID userId) {
        AuthUser authUser = new AuthUser(userId, UserRole.USER, UUID.randomUUID());
        return new UsernamePasswordAuthenticationToken(
                authUser, null, authUser.authorities());
    }
}
