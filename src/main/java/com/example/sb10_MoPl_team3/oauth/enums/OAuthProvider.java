package com.example.sb10_MoPl_team3.oauth.enums;

import java.util.Arrays;

public enum OAuthProvider {
    GOOGLE,
    KAKAO;

    public static OAuthProvider fromRegistrationId(String registrationId) {
        return Arrays.stream(values())
                .filter(provider -> provider.name().equalsIgnoreCase(registrationId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported OAuth provider: " + registrationId));
    }
}