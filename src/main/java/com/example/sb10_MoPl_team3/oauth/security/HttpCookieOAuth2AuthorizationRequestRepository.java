package com.example.sb10_MoPl_team3.oauth.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.io.Decoders;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "OAUTH2_AUTHORIZATION_REQUEST";
    private static final Duration COOKIE_MAX_AGE = Duration.ofMinutes(3);

    private final boolean secureCookie;
    private final byte[] signingKey;
    private final ObjectMapper objectMapper;

    public HttpCookieOAuth2AuthorizationRequestRepository(
            @Value("${auth.refresh-token-cookie.secure:true}") boolean secureCookie,
            @Value("${jwt.secret}") String signingSecret,
            ObjectMapper objectMapper
    ) {
        this.secureCookie = secureCookie;
        this.signingKey = Decoders.BASE64.decode(signingSecret);
        this.objectMapper = objectMapper;
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);

        if (cookie == null) {
            return null;
        }

        return deserialize(cookie.getValue());
    }

    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (authorizationRequest == null) {
            expireCookie(response);
            return;
        }

        ResponseCookie cookie = ResponseCookie
                .from(OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, serialize(authorizationRequest))
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .maxAge(COOKIE_MAX_AGE)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        expireCookie(response);

        return authorizationRequest;
    }

    private String serialize(OAuth2AuthorizationRequest authorizationRequest) {
        AuthorizationRequestCookie payload = AuthorizationRequestCookie.from(authorizationRequest);

        try {
            String encodedPayload = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(payload));

            return encodedPayload + "." + sign(encodedPayload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize OAuth authorization request", exception);
        }
    }

    private OAuth2AuthorizationRequest deserialize(String value) {
        String[] parts = value.split("\\.", -1);
        if (parts.length != 2 || !isValidSignature(parts[0], parts[1])) {
            return null;
        }

        try {
            byte[] decoded = Base64.getUrlDecoder().decode(parts[0]);
            AuthorizationRequestCookie payload = objectMapper.readValue(decoded, AuthorizationRequestCookie.class);

            return payload.toAuthorizationRequest();
        } catch (RuntimeException | IOException exception) {
            return null;
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to sign OAuth authorization request", exception);
        }
    }

    private boolean isValidSignature(String payload, String signature) {
        return MessageDigest.isEqual(
                sign(payload).getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void expireCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie
                .from(OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private record AuthorizationRequestCookie(
            String authorizationUri,
            String clientId,
            String redirectUri,
            Set<String> scopes,
            String state,
            Map<String, Object> additionalParameters,
            Map<String, Object> attributes
    ) {

        private static AuthorizationRequestCookie from(OAuth2AuthorizationRequest authorizationRequest) {
            return new AuthorizationRequestCookie(
                    authorizationRequest.getAuthorizationUri(),
                    authorizationRequest.getClientId(),
                    authorizationRequest.getRedirectUri(),
                    authorizationRequest.getScopes(),
                    authorizationRequest.getState(),
                    new HashMap<>(authorizationRequest.getAdditionalParameters()),
                    new HashMap<>(authorizationRequest.getAttributes())
            );
        }

        private OAuth2AuthorizationRequest toAuthorizationRequest() {
            return OAuth2AuthorizationRequest.authorizationCode()
                    .authorizationUri(authorizationUri)
                    .clientId(clientId)
                    .redirectUri(redirectUri)
                    .scopes(scopes == null ? Collections.emptySet() : new HashSet<>(scopes))
                    .state(state)
                    .additionalParameters(parameters -> parameters.putAll(
                            additionalParameters == null ? Collections.emptyMap() : additionalParameters
                    ))
                    .attributes(values -> values.putAll(
                            attributes == null ? Collections.emptyMap() : attributes
                    ))
                    .build();
        }
    }
}
