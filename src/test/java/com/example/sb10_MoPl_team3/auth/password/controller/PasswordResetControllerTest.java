package com.example.sb10_MoPl_team3.auth.password.controller;

import com.example.sb10_MoPl_team3.auth.password.dto.TemporaryPasswordIssueRequest;
import com.example.sb10_MoPl_team3.auth.password.service.PasswordResetService;
import com.example.sb10_MoPl_team3.global.config.SecurityConfig;
import com.example.sb10_MoPl_team3.global.exception.GlobalExceptionHandler;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtProvider;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtSessionValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PasswordResetController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class PasswordResetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private JwtSessionValidator jwtSessionValidator;

    @Test
    @DisplayName("이메일이 유효하면 임시 비밀번호 발급 요청에 성공한다")
    void issue_success() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@test.com"
                                }
                                """))
                .andExpect(status().isNoContent());

        ArgumentCaptor<TemporaryPasswordIssueRequest> requestCaptor =
                ArgumentCaptor.forClass(TemporaryPasswordIssueRequest.class);

        then(passwordResetService).should()
                .issueTemporaryPassword(requestCaptor.capture());

        assertThat(requestCaptor.getValue().email()).isEqualTo("user@test.com");
    }

    @Test
    @DisplayName("이메일이 비어 있으면 임시 비밀번호 발급 요청에 실패한다")
    void issue_blankEmail() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": ""
                                }
                                """))
                .andExpect(status().isBadRequest());

        then(passwordResetService).should(never())
                .issueTemporaryPassword(any());
    }

    @Test
    @DisplayName("이메일 형식이 아니면 임시 비밀번호 발급 요청에 실패한다")
    void issue_invalidEmail() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "invalid-email"
                                }
                                """))
                .andExpect(status().isBadRequest());

        then(passwordResetService).should(never())
                .issueTemporaryPassword(any());
    }

    @Test
    @DisplayName("CSRF 토큰이 없으면 임시 비밀번호 발급 요청에 실패한다")
    void issue_withoutCsrf() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@test.com"
                                }
                                """))
                .andExpect(status().isForbidden());

        then(passwordResetService).should(never())
                .issueTemporaryPassword(any());
    }
}
