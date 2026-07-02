package com.example.sb10_MoPl_team3.global.security.jwt;

import com.example.sb10_MoPl_team3.auth.entity.AuthSession;
import com.example.sb10_MoPl_team3.auth.repository.AuthSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtSessionValidator {

    private final AuthSessionRepository authSessionRepository;
    private final Clock clock;

    public void validate(JwtClaims claims) {
        Objects.requireNonNull(claims, "claims must not be null");

        UUID sessionId = claims.sessionId();
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId must not be null");
        }

        AuthSession session = authSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Auth session not found"));

        if (!session.getUserId().equals(claims.userId())) {
            throw new IllegalArgumentException("Auth session user mismatch");
        }

        if (session.isRevoked()) {
            throw new IllegalArgumentException("Auth session is revoked");
        }

        Instant now = Instant.now(clock);
        if (!session.getExpiresAt().isAfter(now)) {
            throw new IllegalArgumentException("Auth session is expired");
        }
    }
}