package com.example.sb10_MoPl_team3.oauth.dto;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public record OAuthUserPrincipal(
        OAuthUserInfo userInfo,
        Map<String, Object> attributes,
        Collection<? extends GrantedAuthority> authorities
) implements OAuth2User {

    public OAuthUserPrincipal {
        Objects.requireNonNull(userInfo, "userInfo must not be null");
        Objects.requireNonNull(attributes, "attributes must not be null");
        Objects.requireNonNull(authorities, "authorities must not be null");
    }

    @Override
    public String getName() {
        return userInfo.providerUserId();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
}