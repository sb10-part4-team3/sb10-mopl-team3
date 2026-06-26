package com.example.sb10_MoPl_team3.global.security.jwt;

import com.example.sb10_MoPl_team3.global.security.AuthUser;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 인증 정보 없이 다음 필터로 진행한다")
    void noAuthorizationHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        assertThat(authentication).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Bearer Access Token이 유효하면 SecurityContext에 AuthUser 인증 정보를 저장한다")
    void validAccessToken() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        String token = "valid-access-token";

        JwtClaims claims = new JwtClaims(
                userId,
                UserRole.USER,
                JwtTokenType.ACCESS,
                sessionId,
                Instant.parse("2026-06-24T00:00:00Z"),
                Instant.parse("2026-06-24T01:00:00Z")
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtProvider.parseAccessToken(token)).thenReturn(claims);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal()).isInstanceOf(AuthUser.class);

        AuthUser authUser = (AuthUser) authentication.getPrincipal();

        assertThat(authUser.userId()).isEqualTo(userId);
        assertThat(authUser.role()).isEqualTo(UserRole.USER);
        assertThat(authUser.sessionId()).isEqualTo(sessionId);
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("SSE 구독 요청은 accessToken 쿼리 파라미터로 인증할 수 있다")
    void sseSubscribeWithAccessTokenQueryParameter() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        String token = "valid-sse-access-token";

        JwtClaims claims = new JwtClaims(
                userId,
                UserRole.USER,
                JwtTokenType.ACCESS,
                sessionId,
                Instant.parse("2026-06-24T00:00:00Z"),
                Instant.parse("2026-06-24T01:00:00Z")
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setServletPath("/api/sse");
        request.addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE);
        request.addParameter("accessToken", token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtProvider.parseAccessToken(token)).thenReturn(claims);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(AuthUser.class);

        AuthUser authUser = (AuthUser) authentication.getPrincipal();

        assertThat(authUser.userId()).isEqualTo(userId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("SSE 구독 요청은 access_token 쿼리 파라미터 별칭으로 인증할 수 있다")
    void sseSubscribeWithSnakeCaseAccessTokenQueryParameter() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        String token = "valid-sse-access-token";

        JwtClaims claims = new JwtClaims(
                userId,
                UserRole.USER,
                JwtTokenType.ACCESS,
                sessionId,
                Instant.parse("2026-06-24T00:00:00Z"),
                Instant.parse("2026-06-24T01:00:00Z")
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setServletPath("/api/sse");
        request.addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE);
        request.addParameter("access_token", token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtProvider.parseAccessToken(token)).thenReturn(claims);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(AuthUser.class);

        AuthUser authUser = (AuthUser) authentication.getPrincipal();

        assertThat(authUser.userId()).isEqualTo(userId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("SSE 구독 요청의 Accept 헤더는 대소문자와 파라미터가 달라도 허용한다")
    void sseSubscribeWithCaseInsensitiveAcceptHeader() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        String token = "valid-sse-access-token";

        JwtClaims claims = new JwtClaims(
                userId,
                UserRole.USER,
                JwtTokenType.ACCESS,
                sessionId,
                Instant.parse("2026-06-24T00:00:00Z"),
                Instant.parse("2026-06-24T01:00:00Z")
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setServletPath("/api/sse");
        request.addHeader(HttpHeaders.ACCEPT, "TEXT/EVENT-STREAM;charset=UTF-8");
        request.addParameter("accessToken", token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtProvider.parseAccessToken(token)).thenReturn(claims);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        assertThat(authentication).isNotNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Accept 헤더가 text/event-stream 부분 문자열만 포함하면 SSE 쿼리 토큰 인증을 하지 않는다")
    void accessTokenQueryParameterIsIgnoredWithNonExactSseAcceptHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setServletPath("/api/sse");
        request.addHeader(HttpHeaders.ACCEPT, "application/text/event-stream+json");
        request.addParameter("accessToken", "query-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(jwtProvider, never()).parseAccessToken("query-token");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("SSE 구독 요청의 accessToken 쿼리 파라미터가 중복되면 401을 반환한다")
    void sseSubscribeWithDuplicatedAccessTokenQueryParameter() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setServletPath("/api/sse");
        request.addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE);
        request.addParameter("accessToken", "first-token", "second-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(jwtProvider, never()).parseAccessToken("first-token");
        verify(jwtProvider, never()).parseAccessToken("second-token");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("SSE 구독 요청의 accessToken과 access_token이 함께 있으면 401을 반환한다")
    void sseSubscribeWithMixedTokenQueryParameters() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setServletPath("/api/sse");
        request.addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE);
        request.addParameter("accessToken", "camel-case-token");
        request.addParameter("access_token", "snake-case-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(jwtProvider, never()).parseAccessToken("camel-case-token");
        verify(jwtProvider, never()).parseAccessToken("snake-case-token");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("SSE 구독 요청이 아니면 accessToken 쿼리 파라미터를 인증에 사용하지 않는다")
    void accessTokenQueryParameterIsIgnoredOutsideSseSubscribe() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setServletPath("/api/test/protected/me");
        request.addParameter("accessToken", "query-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(jwtProvider, never()).parseAccessToken("query-token");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Bearer Access Token이 유효하지 않으면 401을 반환하고 다음 필터로 진행하지 않는다")
    void invalidAccessToken() throws Exception {
        String token = "invalid-access-token";

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtProvider.parseAccessToken(token)).thenThrow(new JwtException("Invalid token"));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Authorization 헤더가 Bearer 형식이 아니면 인증 정보 없이 다음 필터로 진행한다")
    void notBearerToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(filterChain).doFilter(request, response);
    }
}
