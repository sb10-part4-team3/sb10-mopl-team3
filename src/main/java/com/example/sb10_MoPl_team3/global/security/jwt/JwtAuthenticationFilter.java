package com.example.sb10_MoPl_team3.global.security.jwt;

import com.example.sb10_MoPl_team3.global.security.AuthUser;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String SSE_SUBSCRIBE_PATH = "/api/sse";
    private static final String ACCESS_TOKEN_PARAM = "accessToken";
    private static final String ACCESS_TOKEN_SNAKE_CASE_PARAM = "access_token";

    private final JwtProvider jwtProvider;

    public JwtAuthenticationFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = extractToken(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (token.isEmpty()) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        try {
            JwtClaims claims = jwtProvider.parseAccessToken(token);
            AuthUser authUser = AuthUser.from(claims);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            authUser,
                            null,
                            authUser.authorities()
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);


        } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length()).trim();
        }

        if (isSseSubscribeRequest(request)) {
            String accessToken = request.getParameter(ACCESS_TOKEN_PARAM);
            if (accessToken == null || accessToken.isBlank()) {
                accessToken = request.getParameter(ACCESS_TOKEN_SNAKE_CASE_PARAM);
            }

            return accessToken == null ? null : accessToken.trim();
        }

        return null;
    }

    private boolean isSseSubscribeRequest(HttpServletRequest request) {
        String accept = request.getHeader(HttpHeaders.ACCEPT);

        return "GET".equals(request.getMethod())
                && SSE_SUBSCRIBE_PATH.equals(request.getServletPath())
                && accept != null
                && accept.contains(MediaType.TEXT_EVENT_STREAM_VALUE);
    }
}
