package com.example.sb10_MoPl_team3.auth.controller;

import com.example.sb10_MoPl_team3.global.config.SecurityConfig;
import com.example.sb10_MoPl_team3.global.exception.GlobalExceptionHandler;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtProvider;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtSessionValidator;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CsrfController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class CsrfControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private JwtSessionValidator jwtSessionValidator;

    @Test
    @DisplayName("CSRF 토큰 조회 요청이 성공하면 XSRF-TOKEN 쿠키를 발급한다")
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
}
