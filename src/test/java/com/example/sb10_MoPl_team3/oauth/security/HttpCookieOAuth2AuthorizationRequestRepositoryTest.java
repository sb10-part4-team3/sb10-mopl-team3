package com.example.sb10_MoPl_team3.oauth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class HttpCookieOAuth2AuthorizationRequestRepositoryTest {

    private static final String COOKIE_NAME = "OAUTH2_AUTHORIZATION_REQUEST";
    private static final String SIGNING_SECRET = Base64.getEncoder()
            .encodeToString("test-oauth-cookie-signing-secret".getBytes(StandardCharsets.UTF_8));

    private final HttpCookieOAuth2AuthorizationRequestRepository repository =
            new HttpCookieOAuth2AuthorizationRequestRepository(true, SIGNING_SECRET, new ObjectMapper());

    @Test
    @DisplayName("OAuth 인증 요청을 쿠키에 저장한다")
    void save() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        repository.saveAuthorizationRequest(
                authorizationRequest(),
                new MockHttpServletRequest(),
                response
        );

        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);

        assertThat(setCookie).contains(COOKIE_NAME + "=");
        assertThat(setCookie).contains("Path=/");
        assertThat(setCookie).contains("Max-Age=180");
        assertThat(setCookie).contains("Secure");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("SameSite=Lax");
    }

    @Test
    @DisplayName("쿠키에 저장된 OAuth 인증 요청을 다시 읽을 수 있다")
    void load() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2AuthorizationRequest authorizationRequest = authorizationRequest();

        repository.saveAuthorizationRequest(
                authorizationRequest,
                new MockHttpServletRequest(),
                response
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(COOKIE_NAME, extractCookieValue(response)));

        OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(request);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getAuthorizationUri()).isEqualTo(authorizationRequest.getAuthorizationUri());
        assertThat(loaded.getClientId()).isEqualTo(authorizationRequest.getClientId());
        assertThat(loaded.getRedirectUri()).isEqualTo(authorizationRequest.getRedirectUri());
        assertThat(loaded.getState()).isEqualTo(authorizationRequest.getState());
        assertThat(loaded.getScopes()).containsExactlyInAnyOrderElementsOf(authorizationRequest.getScopes());
    }

    @Test
    @DisplayName("OAuth 인증 요청을 제거하면 기존 값을 반환하고 쿠키를 만료한다")
    void remove() {
        MockHttpServletResponse saveResponse = new MockHttpServletResponse();

        repository.saveAuthorizationRequest(
                authorizationRequest(),
                new MockHttpServletRequest(),
                saveResponse
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(COOKIE_NAME, extractCookieValue(saveResponse)));

        MockHttpServletResponse removeResponse = new MockHttpServletResponse();

        OAuth2AuthorizationRequest removed = repository.removeAuthorizationRequest(request, removeResponse);

        assertThat(removed).isNotNull();
        assertThat(removed.getState()).isEqualTo("oauth-state");

        String setCookie = removeResponse.getHeader(HttpHeaders.SET_COOKIE);

        assertThat(setCookie).contains(COOKIE_NAME + "=");
        assertThat(setCookie).contains("Max-Age=0");
    }

    @Test
    @DisplayName("저장할 OAuth 인증 요청이 null이면 쿠키를 만료한다")
    void saveNull() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        repository.saveAuthorizationRequest(
                null,
                new MockHttpServletRequest(),
                response
        );

        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);

        assertThat(setCookie).contains(COOKIE_NAME + "=");
        assertThat(setCookie).contains("Max-Age=0");
    }

    @Test
    @DisplayName("Tampered OAuth authorization request cookie is rejected")
    void loadTamperedCookie() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        repository.saveAuthorizationRequest(
                authorizationRequest(),
                new MockHttpServletRequest(),
                response
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(COOKIE_NAME, extractCookieValue(response) + "tampered"));

        OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(request);

        assertThat(loaded).isNull();
    }

    private OAuth2AuthorizationRequest authorizationRequest() {
        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .clientId("google-client-id")
                .redirectUri("https://moduplaylist.site/login/oauth2/code/google")
                .scopes(Set.of("openid", "email", "profile"))
                .state("oauth-state")
                .build();
    }

    private String extractCookieValue(MockHttpServletResponse response) {
        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        String prefix = COOKIE_NAME + "=";

        int startIndex = setCookie.indexOf(prefix) + prefix.length();
        int endIndex = setCookie.indexOf(';', startIndex);

        return setCookie.substring(startIndex, endIndex);
    }
}
