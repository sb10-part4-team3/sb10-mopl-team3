package com.example.sb10_MoPl_team3.auth.controller;

import com.example.sb10_MoPl_team3.auth.dto.AuthTokenResult;
import com.example.sb10_MoPl_team3.auth.dto.response.JwtDto;
import com.example.sb10_MoPl_team3.auth.exception.InvalidRefreshTokenException;
import com.example.sb10_MoPl_team3.auth.service.AuthService;
import com.example.sb10_MoPl_team3.global.security.AuthUser;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtProvider;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtProperties;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtSessionValidator;
import com.example.sb10_MoPl_team3.user.dto.response.UserDto;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.global.config.SecurityConfig;
import com.example.sb10_MoPl_team3.global.exception.GlobalExceptionHandler;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.http.HttpHeaders.SET_COOKIE;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private JwtSessionValidator jwtSessionValidator;

    @MockitoBean
    private JwtProperties jwtProperties;

    @Test
    @DisplayName("로그인 요청이 유효하면 사용자 정보와 액세스 토큰을 반환한다")
    void signIn_success() throws Exception {
        UserDto userDto = new UserDto(
                UUID.randomUUID(),
                Instant.parse("2026-06-23T00:00:00Z"),
                "user@test.com",
                "홍길동",
                null,
                UserRole.USER,
                false
        );

        given(authService.signIn(any()))
                .willReturn(new AuthTokenResult(new JwtDto(userDto, "access-token"), "refresh-token"));
        given(jwtProperties.refreshTokenExpiration()).willReturn(Duration.ofDays(7));

        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(APPLICATION_JSON)
                        .with(csrf())
                        .content("""
                                {
                                  "email": "user@test.com",
                                  "password": "password1!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(SET_COOKIE, containsString("REFRESH_TOKEN=refresh-token")))
                .andExpect(header().string(SET_COOKIE, containsString("HttpOnly")))
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.userDto.id").exists())
                .andExpect(jsonPath("$.userDto.createdAt").exists())
                .andExpect(jsonPath("$.userDto.email").value("user@test.com"))
                .andExpect(jsonPath("$.userDto.name").value("홍길동"))
                .andExpect(jsonPath("$.userDto.profileImageUrl").doesNotExist())
                .andExpect(jsonPath("$.userDto.role").value("USER"))
                .andExpect(jsonPath("$.userDto.locked").value(false));
    }

    @Test
    @DisplayName("form 로그인 요청이 유효하면 사용자 정보와 액세스 토큰을 반환한다")
    void signIn_form_success() throws Exception {
        UserDto userDto = new UserDto(
                UUID.randomUUID(),
                Instant.parse("2026-06-23T00:00:00Z"),
                "user@test.com",
                "Test User",
                null,
                UserRole.USER,
                false
        );

        given(authService.signIn(any()))
                .willReturn(new AuthTokenResult(new JwtDto(userDto, "access-token"), "refresh-token"));
        given(jwtProperties.refreshTokenExpiration()).willReturn(Duration.ofDays(7));

        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(APPLICATION_FORM_URLENCODED)
                        .with(csrf())
                        .param("username", "user@test.com")
                        .param("password", "password1!"))
                .andExpect(status().isOk())
                .andExpect(header().string(SET_COOKIE, containsString("REFRESH_TOKEN=refresh-token")))
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.userDto.email").value("user@test.com"));
    }

    @Test
    @DisplayName("refresh token 쿠키가 유효하면 새 토큰을 발급한다")
    void refresh_success() throws Exception {
        UserDto userDto = new UserDto(
                UUID.randomUUID(),
                Instant.parse("2026-06-23T00:00:00Z"),
                "user@test.com",
                "Test User",
                null,
                UserRole.USER,
                false
        );

        given(authService.reissueToken("refresh-token"))
                .willReturn(new AuthTokenResult(new JwtDto(userDto, "new-access-token"), "new-refresh-token"));
        given(jwtProperties.refreshTokenExpiration()).willReturn(Duration.ofDays(7));

        mockMvc.perform(post("/api/auth/refresh")
                        .with(csrf())
                        .cookie(new Cookie("REFRESH_TOKEN", "refresh-token")))
                .andExpect(status().isOk())
                .andExpect(header().string(SET_COOKIE, containsString("REFRESH_TOKEN=new-refresh-token")))
                .andExpect(header().string(SET_COOKIE, containsString("HttpOnly")))
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.userDto.email").value("user@test.com"));
    }

    @Test
    @DisplayName("refresh token 쿠키가 없으면 401을 반환한다")
    void refresh_missingCookie() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIAL"));
    }

    @Test
    @DisplayName("refresh token이 유효하지 않으면 401을 반환한다")
    void refresh_invalidToken() throws Exception {
        given(authService.reissueToken("invalid-refresh-token"))
                .willThrow(new InvalidRefreshTokenException());

        mockMvc.perform(post("/api/auth/refresh")
                        .with(csrf())
                        .cookie(new Cookie("REFRESH_TOKEN", "invalid-refresh-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIAL"));
    }

    @Test
    @DisplayName("CSRF 토큰 없이 refresh 요청하면 403을 반환한다")
    void refresh_withoutCsrf() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("REFRESH_TOKEN", "refresh-token")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("로그인 요청 값이 유효하지 않으면 400을 반환한다")
    void signIn_invalid() throws Exception {
        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(APPLICATION_JSON)
                        .with(csrf())
                        .content("""
                                {
                                  "email": "",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    @DisplayName("인증된 사용자가 로그아웃하면 세션을 무효화하고 refresh token 쿠키를 만료한다")
    void signOut_success() throws Exception {
        AuthUser authUser = new AuthUser(
                UUID.randomUUID(),
                UserRole.USER,
                UUID.randomUUID()
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        authUser,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );

        mockMvc.perform(post("/api/auth/sign-out")
                        .with(csrf())
                        .with(authentication(authentication)))
                .andExpect(status().isNoContent())
                .andExpect(header().string(SET_COOKIE, containsString("REFRESH_TOKEN=")))
                .andExpect(header().string(SET_COOKIE, containsString("Max-Age=0")))
                .andExpect(header().string(SET_COOKIE, containsString("HttpOnly")));

        then(authService).should().signOut(authUser);
    }

    @Test
    @DisplayName("인증 정보 없이 로그아웃하면 401을 반환한다")
    void signOut_unauthenticated() throws Exception {
        mockMvc.perform(post("/api/auth/sign-out")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
