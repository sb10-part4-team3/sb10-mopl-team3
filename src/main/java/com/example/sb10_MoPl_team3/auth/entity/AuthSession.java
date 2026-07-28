package com.example.sb10_MoPl_team3.auth.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RedisHash("auth_sessions")
public class AuthSession {

    @Id
    private UUID id;

    @Indexed
    private UUID userId;

    @Indexed
    private String refreshTokenHash;

    private Instant expiresAt;

    private boolean revoked;

    private Instant revokedAt;

    private Instant createdAt;

    @TimeToLive
    private Long ttlSeconds;

    @Builder
    private AuthSession(
            UUID id,
            UUID userId,
            String refreshTokenHash,
            Instant expiresAt,
            boolean revoked,
            Instant revokedAt,
            Instant createdAt,
            Long ttlSeconds
    ) {
        this.id = id;
        this.userId = userId;
        this.refreshTokenHash = refreshTokenHash;
        this.expiresAt = expiresAt;
        this.revoked = revoked;
        this.revokedAt = revokedAt;
        this.createdAt = createdAt;
        this.ttlSeconds = ttlSeconds;
    }

    public static AuthSession create(
            UUID userId,
            String refreshTokenHash,
            Instant expiresAt,
            Instant now
    ) {
        long ttlSeconds = Duration.between(now, expiresAt).toSeconds();
        if (ttlSeconds <= 0) {
            throw new IllegalArgumentException("expiresAt must be after now");
        }

        return AuthSession.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .refreshTokenHash(refreshTokenHash)
                .expiresAt(expiresAt)
                .revoked(false)
                .revokedAt(null)
                .createdAt(now)
                .ttlSeconds(ttlSeconds)
                .build();
    }

    public void revoke(Instant now) {
        if (revoked) {
            return;
        }

        this.revoked = true;
        this.revokedAt = now;
    }

    public void rotateRefreshToken(String refreshTokenHash, Instant expiresAt, Instant now) {
        long ttlSeconds = Duration.between(now, expiresAt).toSeconds();
        if (ttlSeconds <= 0) {
            throw new IllegalArgumentException("expiresAt must be after now");
        }

        this.refreshTokenHash = refreshTokenHash;
        this.expiresAt = expiresAt;
        this.ttlSeconds = ttlSeconds;
    }
}
