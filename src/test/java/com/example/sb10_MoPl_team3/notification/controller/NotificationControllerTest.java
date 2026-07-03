package com.example.sb10_MoPl_team3.notification.controller;

import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.sb10_MoPl_team3.global.config.SecurityConfig;
import com.example.sb10_MoPl_team3.global.exception.GlobalExceptionHandler;
import com.example.sb10_MoPl_team3.global.security.AuthUser;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtProvider;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtSessionValidator;
import com.example.sb10_MoPl_team3.notification.service.NotificationService;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
