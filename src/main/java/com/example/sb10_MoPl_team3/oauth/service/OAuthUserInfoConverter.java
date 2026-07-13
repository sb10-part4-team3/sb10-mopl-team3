package com.example.sb10_MoPl_team3.oauth.service;

import com.example.sb10_MoPl_team3.oauth.dto.OAuthUserInfo;
import com.example.sb10_MoPl_team3.oauth.enums.OAuthProvider;
import org.springframework.stereotype.Component;

import java.util.Collections;
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

        return map.entrySet().stream()
                .filter(entry -> entry.getKey() instanceof String)
                .collect(
                        java.util.stream.Collectors.toMap(
                                entry -> (String) entry.getKey(),
                                Map.Entry::getValue
                        )
                );
    }
}