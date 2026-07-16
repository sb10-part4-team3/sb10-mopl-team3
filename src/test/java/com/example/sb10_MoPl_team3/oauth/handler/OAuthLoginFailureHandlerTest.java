package com.example.sb10_MoPl_team3.oauth.handler;

import com.example.sb10_MoPl_team3.oauth.config.OAuthRedirectProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthLoginFailureHandlerTest {

    private final OAuthLoginFailureHandler failureHandler = new OAuthLoginFailureHandler(
            new OAuthRedirectProperties(
                    "https://moduplaylist.site/",
                    "https://moduplaylist.site/#/sign-in"
            )
    );

    @Test
    @DisplayName("OAuth2 예외가 발생하면 error code를 포함해 실패 리다이렉트한다")
    void oauth2Failure() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        failureHandler.onAuthenticationFailure(
                new MockHttpServletRequest(),
                response,
                new OAuth2AuthenticationException(new OAuth2Error("access_denied"))
        );

        assertThat(response.getRedirectedUrl())
                .isEqualTo("https://moduplaylist.site/#/sign-in?error=oauth_failed&error_message=access_denied");
    }

    @Test
    @DisplayName("일반 인증 예외가 발생하면 기본 OAuth 실패 메시지로 리다이렉트한다")
    void generalFailure() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        failureHandler.onAuthenticationFailure(
                new MockHttpServletRequest(),
                response,
                new BadCredentialsException("bad credentials")
        );

        assertThat(response.getRedirectedUrl())
                .isEqualTo("https://moduplaylist.site/#/sign-in?error=oauth_failed&error_message=oauth_failed");
    }
}