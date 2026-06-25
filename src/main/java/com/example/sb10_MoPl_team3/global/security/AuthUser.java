package com.example.sb10_MoPl_team3.global.security;

import com.example.sb10_MoPl_team3.global.security.jwt.JwtClaims;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record AuthUser(
        UUID userId,
        UserRole role,
        UUID sessionId
) implements Principal {

    public AuthUser {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(role, "role must not be null");
        // sessionId 미구현
    }

    public static AuthUser from(JwtClaims claims) {
        Objects.requireNonNull(claims, "claims must not be null");

        return new AuthUser(
                claims.userId(),
                claims.role(),
                claims.sessionId()
        );
    }

    public Collection<? extends GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    @Override
    public String getName() {
        return userId.toString();
    }
}
