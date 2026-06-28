package com.example.sb10_MoPl_team3.auth.integration;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CsrfIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("CSRF 토큰 조회 요청은 XSRF-TOKEN 쿠키를 발급한다")
    void getCsrfToken_success() throws Exception {
        mockMvc.perform(get("/api/auth/csrf-token"))
                .andExpect(status().isNoContent())
                .andExpect(result -> {
                    Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");

                    assertThat(cookie).isNotNull();
                    assertThat(cookie.getValue()).isNotBlank();
                    assertThat(cookie.isHttpOnly()).isFalse();
                    assertThat(cookie.getPath()).isEqualTo("/");
                });
    }

    @Test
    @DisplayName("CSRF 토큰 없이 상태 변경 요청을 보내면 403을 반환한다")
    void postWithoutCsrf_forbidden() throws Exception {
        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@test.com",
                                  "password": "password1!"
                                }
                                """))
                .andExpect(status().isForbidden());
    }
}