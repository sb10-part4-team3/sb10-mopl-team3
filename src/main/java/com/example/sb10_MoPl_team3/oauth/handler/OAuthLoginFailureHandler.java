package com.example.sb10_MoPl_team3.oauth.handler;

import com.example.sb10_MoPl_team3.oauth.config.OAuthRedirectProperties;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class OAuthLoginFailureHandler implements AuthenticationFailureHandler {

    private final OAuthRedirectProperties redirectProperties;

    public OAuthLoginFailureHandler(OAuthRedirectProperties redirectProperties) {
        this.redirectProperties = redirectProperties;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        response.sendRedirect(createFailureRedirectUri(resolveErrorMessage(exception)));
    }

    private String resolveErrorMessage(AuthenticationException exception) {
        if (exception instanceof OAuth2AuthenticationException oauth2Exception) {
            return oauth2Exception.getError().getErrorCode();
        }

        return "oauth_failed";
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