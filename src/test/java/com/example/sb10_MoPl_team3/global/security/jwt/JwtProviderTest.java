package com.example.sb10_MoPl_team3.global.security.jwt;

import com.example.sb10_MoPl_team3.user.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private final Instant fixedInstant = Instant.parse("2026-06-24T00:00:00Z");
    private final Clock fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC);

    private final JwtProperties jwtProperties = new JwtProperties(
            "test-secret-key-for-jwt-provider-test-must-be-long-enough",
            Duration.ofHours(1),
            "mopl-test"
    );

    private final JwtProvider jwtProvider = new JwtProvider(jwtProperties, fixedClock);

    @Test
    @DisplayName("Access Token을 생성하고 다시 파싱하면 claim 정보를 복원한다")
    void generateAndParseAccessToken() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        String token = jwtProvider.generateAccessToken(
                userId,
                UserRole.USER,
                sessionId
        );

        JwtClaims claims = jwtProvider.parseAccessToken(token);

        assertThat(claims.userId()).isEqualTo(userId);
        assertThat(claims.role()).isEqualTo(UserRole.USER);
        assertThat(claims.type()).isEqualTo(JwtTokenType.ACCESS);
        assertThat(claims.sessionId()).isEqualTo(sessionId);
        assertThat(claims.issuedAt()).isEqualTo(fixedInstant);
        assertThat(claims.expiresAt()).isEqualTo(fixedInstant.plus(Duration.ofHours(1)));
    }
}