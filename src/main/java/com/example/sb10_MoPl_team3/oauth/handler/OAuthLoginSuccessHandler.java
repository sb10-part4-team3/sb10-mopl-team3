package com.example.sb10_MoPl_team3.oauth.handler;

import com.example.sb10_MoPl_team3.auth.dto.AuthTokenResult;
import com.example.sb10_MoPl_team3.auth.exception.InvalidCredentialException;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtProperties;
import com.example.sb10_MoPl_team3.oauth.config.OAuthRedirectProperties;
import com.example.sb10_MoPl_team3.oauth.dto.OAuthUserPrincipal;
import com.example.sb10_MoPl_team3.oauth.service.OAuthAuthenticationService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class OAuthLoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "REFRESH_TOKEN";
    private static final String AUTH_COOKIE_PATH = "/api/auth";

    private final OAuthAuthenticationService oauthAuthenticationService;
    private final JwtProperties jwtProperties;
    private final OAuthRedirectProperties redirectProperties;
    private final boolean refreshTokenCookieSecure;

    public OAuthLoginSuccessHandler(
            OAuthAuthenticationService oauthAuthenticationService,
            JwtProperties jwtProperties,
            OAuthRedirectProperties redirectProperties,
            @Value("${auth.refresh-token-cookie.secure:true}") boolean refreshTokenCookieSecure
    ) {
        this.oauthAuthenticationService = oauthAuthenticationService;
        this.jwtProperties = jwtProperties;
        this.redirectProperties = redirectProperties;
        this.refreshTokenCookieSecure = refreshTokenCookieSecure;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof OAuthUserPrincipal principal)) {
            redirectFailure(response, "invalid_oauth_principal");
            return;
        }

        try {
            AuthTokenResult tokenResult = oauthAuthenticationService.signin(principal.userInfo());

            response.addHeader(
                    HttpHeaders.SET_COOKIE,
                    createRefreshTokenCookie(tokenResult.refreshToken()).toString()
            );

            response.sendRedirect(redirectProperties.successUri());
        } catch (InvalidCredentialException exception) {
            redirectFailure(response, "invalid_credential");
        } catch (OAuth2AuthenticationException exception) {
            redirectFailure(response, exception.getError().getErrorCode());
        }
    }

    private ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(refreshTokenCookieSecure)
                .path(AUTH_COOKIE_PATH)
                .maxAge(jwtProperties.refreshTokenExpiration())
                .sameSite("Lax")
                .build();
    }

    private void redirectFailure(HttpServletResponse response, String errorMessage) throws IOException {
        response.sendRedirect(createFailureRedirectUri(errorMessage));
    }

    private String createFailureRedirectUri(String errorMessage) {
        String failureUri = redirectProperties.failureUri();
        String separator = failureUri.contains("?") ? "&" : "?";

        return failureUri
                + separator
                + "error=oauth_failed"
                + "&error_message="
                + UriUtils.encodeQueryParam(errorMessage, StandardCharsets.UTF_8);
    }
}