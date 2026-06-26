package com.example.sb10_MoPl_team3.global.security.jwt;

import com.example.sb10_MoPl_team3.global.security.AuthUser;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
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
        if (isSseSubscribeRequest(request) && hasDuplicatedSseTokenParameter(request)) {
            return "";
        }

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length()).trim();
        }

        if (isSseSubscribeRequest(request)) {
            return extractSseQueryToken(request);
        }

        return null;
    }

    private String extractSseQueryToken(HttpServletRequest request) {
        String[] camelCaseTokens = request.getParameterValues(ACCESS_TOKEN_PARAM);
        if (camelCaseTokens != null && camelCaseTokens.length == 1) {
            return camelCaseTokens[0].trim();
        }

        String[] snakeCaseTokens = request.getParameterValues(ACCESS_TOKEN_SNAKE_CASE_PARAM);
        if (snakeCaseTokens != null && snakeCaseTokens.length == 1) {
            return snakeCaseTokens[0].trim();
        }

        return null;
    }

    private boolean hasDuplicatedSseTokenParameter(HttpServletRequest request) {
        return countParameterValues(request, ACCESS_TOKEN_PARAM)
                + countParameterValues(request, ACCESS_TOKEN_SNAKE_CASE_PARAM) > 1;
    }

    private int countParameterValues(HttpServletRequest request, String parameterName) {
        String[] values = request.getParameterValues(parameterName);

        return values == null ? 0 : values.length;
    }

    private boolean isSseSubscribeRequest(HttpServletRequest request) {
        String accept = request.getHeader(HttpHeaders.ACCEPT);

        return "GET".equals(request.getMethod())
                && SSE_SUBSCRIBE_PATH.equals(request.getServletPath())
                && accept != null
                && hasTextEventStreamAccept(accept);
    }

    private boolean hasTextEventStreamAccept(String accept) {
        try {
            return MediaType.parseMediaTypes(accept).stream()
                    .anyMatch(mediaType ->
                            "text".equalsIgnoreCase(mediaType.getType())
                                    && "event-stream".equalsIgnoreCase(mediaType.getSubtype()));
        } catch (InvalidMediaTypeException e) {
            return false;
        }
    }
}
