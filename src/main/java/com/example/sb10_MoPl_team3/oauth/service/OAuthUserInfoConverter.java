package com.example.sb10_MoPl_team3.oauth.service;

import com.example.sb10_MoPl_team3.oauth.dto.OAuthUserInfo;
import com.example.sb10_MoPl_team3.oauth.enums.OAuthProvider;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class OAuthUserInfoConverter {

    public OAuthUserInfo convert(OAuthProvider provider, Map<String, Object> attributes) {
        return switch (provider) {
            case GOOGLE -> convertGoogle(attributes);
            case KAKAO -> convertKakao(attributes);
        };
    }

    private OAuthUserInfo convertGoogle(Map<String, Object> attributes) {
        String providerUserId = requiredString(attributes.get("sub"), "Google sub");
        String email = stringOrNull(attributes.get("email"));
        boolean emailVerified = booleanOrFalse(attributes.get("email_verified"));
        String name = firstNonBlank(
                stringOrNull(attributes.get("name")),
                email,
                providerUserId
        );
        String profileImageUrl = stringOrNull(attributes.get("picture"));

        return new OAuthUserInfo(
                OAuthProvider.GOOGLE,
                providerUserId,
                email,
                emailVerified,
                name,
                profileImageUrl
        );
    }

    private OAuthUserInfo convertKakao(Map<String, Object> attributes) {
        String providerUserId = requiredString(attributes.get("id"), "Kakao id");

        Map<String, Object> properties = mapOrEmpty(attributes.get("properties"));
        Map<String, Object> kakaoAccount = mapOrEmpty(attributes.get("kakao_account"));
        Map<String, Object> profile = mapOrEmpty(kakaoAccount.get("profile"));

        String email = stringOrNull(kakaoAccount.get("email"));
        boolean emailVerified = booleanOrFalse(kakaoAccount.get("is_email_verified"));
        String name = firstNonBlank(
                stringOrNull(properties.get("nickname")),
                stringOrNull(profile.get("nickname")),
                providerUserId
        );
        String profileImageUrl = firstNonBlank(
                stringOrNull(properties.get("profile_image")),
                stringOrNull(profile.get("profile_image_url"))
        );

        return new OAuthUserInfo(
                OAuthProvider.KAKAO,
                providerUserId,
                email,
                emailVerified,
                name,
                profileImageUrl
        );
    }

    private String requiredString(Object value, String fieldName) {
        String converted = stringOrNull(value);

        if (converted == null || converted.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        return converted;
    }

    private String stringOrNull(Object value) {
        if (value == null) {
            return null;
        }

        return String.valueOf(value);
    }

    private boolean booleanOrFalse(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }

        if (value instanceof String string) {
            return Boolean.parseBoolean(string);
        }

        return false;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return null;
    }

    private Map<String, Object> mapOrEmpty(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Collections.emptyMap();
        }

        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() instanceof String key) {
                result.put(key, entry.getValue());
            }
        }

        return result;
    }
}
