package com.example.sb10_MoPl_team3.global.security.jwt;

import com.example.sb10_MoPl_team3.user.enums.UserRole;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record JwtClaims(
        UUID userId,
        UserRole role,
        JwtTokenType type,
        UUID sessionId,
        Instant issuedAt,
        Instant expiresAt
) {
    public JwtClaims {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(issuedAt, "issuedAt must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");

        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("expiresAt must be after issuedAt");
        }
    }
}
