package com.example.sb10_MoPl_team3.oauth.handler;

import com.example.sb10_MoPl_team3.auth.dto.AuthTokenResult;
import com.example.sb10_MoPl_team3.auth.dto.response.JwtDto;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtProperties;
import com.example.sb10_MoPl_team3.oauth.config.OAuthRedirectProperties;
import com.example.sb10_MoPl_team3.oauth.dto.OAuthUserInfo;
import com.example.sb10_MoPl_team3.oauth.dto.OAuthUserPrincipal;
import com.example.sb10_MoPl_team3.oauth.enums.OAuthProvider;
import com.example.sb10_MoPl_team3.oauth.exception.OAuthAccountNotLinkedException;
import com.example.sb10_MoPl_team3.oauth.service.OAuthAuthenticationService;
import com.example.sb10_MoPl_team3.user.dto.response.UserDto;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class OAuthLoginSuccessHandlerTest {

    private final OAuthAuthenticationService oauthAuthenticationService =
            mock(OAuthAuthenticationService.class);

    private final OAuthLoginSuccessHandler successHandler = new OAuthLoginSuccessHandler(
            oauthAuthenticationService,
            new JwtProperties("secret", Duration.ofHours(1), Duration.ofDays(7), "mopl"),
            new OAuthRedirectProperties(
                    "https://moduplaylist.site/",
                    "https://moduplaylist.site/#/sign-in"
            ),
            true
    );

    @Test
    @DisplayName("OAuth 로그인 성공 시 refresh token 쿠키를 설정하고 성공 URI로 리다이렉트한다")
    void success() throws Exception {
        OAuthUserInfo userInfo = new OAuthUserInfo(
                OAuthProvider.GOOGLE,
                "google-user-id",
                "user@test.com",
                "사용자",
                null
        );

        OAuthUserPrincipal principal = new OAuthUserPrincipal(
                userInfo,
                Map.of("sub", "google-user-id"),
                List.of()
        );

        AuthTokenResult tokenResult = new AuthTokenResult(
                new JwtDto(
                        new UserDto(
                                UUID.randomUUID(),
                                Instant.now(),
                                "user@test.com",
                                "사용자",
                                null,
                                UserRole.USER,
                                false
                        ),
                        "access-token"
                ),
                "refresh-token"
        );

        given(oauthAuthenticationService.signin(userInfo)).willReturn(tokenResult);

        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(
                new MockHttpServletRequest(),
                response,
                new TestingAuthenticationToken(principal, null)
        );

        assertThat(response.getRedirectedUrl()).isEqualTo("https://moduplaylist.site/");
        assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
                .contains("REFRESH_TOKEN=refresh-token")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("Path=/api/auth")
                .contains("SameSite=Lax");
    }

    @Test
    @DisplayName("연동되지 않은 OAuth 계정이면 로그인 화면으로 실패 리다이렉트한다")
    void unlinked() throws Exception {
        OAuthUserInfo userInfo = new OAuthUserInfo(
                OAuthProvider.KAKAO,
                "kakao-user-id",
                null,
                "카카오 사용자",
                null
        );

        OAuthUserPrincipal principal = new OAuthUserPrincipal(
                userInfo,
                Map.of("id", 1L),
                List.of()
        );

        given(oauthAuthenticationService.signin(userInfo))
                .willThrow(new OAuthAccountNotLinkedException());

        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(
                new MockHttpServletRequest(),
                response,
                new TestingAuthenticationToken(principal, null)
        );

        assertThat(response.getRedirectedUrl())
                .isEqualTo("https://moduplaylist.site/#/sign-in?error=oauth_failed&error_message=user_not_exists");
    }

    @Test
    @DisplayName("OAuth principal 타입이 올바르지 않으면 실패 리다이렉트한다")
    void invalidPrincipal() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(
                new MockHttpServletRequest(),
                response,
                new TestingAuthenticationToken("invalid-principal", null)
        );

        assertThat(response.getRedirectedUrl())
                .isEqualTo("https://moduplaylist.site/#/sign-in?error=oauth_failed&error_message=invalid_oauth_principal");
    }
}