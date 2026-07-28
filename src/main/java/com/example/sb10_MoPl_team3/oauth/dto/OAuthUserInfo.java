package com.example.sb10_MoPl_team3.oauth.dto;

import com.example.sb10_MoPl_team3.oauth.enums.OAuthProvider;

import java.util.Objects;

public record OAuthUserInfo(
        OAuthProvider provider,
        String providerUserId,
        String email,
        boolean emailVerified,
        String name,
        String profileImageUrl
) {

    public OAuthUserInfo {
        Objects.requireNonNull(provider, "provider must not be null");
        Objects.requireNonNull(providerUserId, "providerUserId must not be null");
        Objects.requireNonNull(name, "name must not be null");
    }

    public String resolvedEmail() {
        if (email != null && !email.isBlank()) {
            return email;
        }

        if (provider == OAuthProvider.KAKAO) {
            return name + "_" + providerUserId + "@kakao.com";
        }

        return null;
    }

    public boolean hasProviderEmail() {
        return email != null && !email.isBlank();
    }
}
