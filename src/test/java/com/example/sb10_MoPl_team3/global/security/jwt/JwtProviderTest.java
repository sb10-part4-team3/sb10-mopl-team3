package com.example.sb10_MoPl_team3.global.security.jwt;

import com.example.sb10_MoPl_team3.user.enums.UserRole;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtProviderTest {

    private static final String TEST_JWT_SECRET =
            "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtcHJvdmlkZXItdGVzdC1tdXN0LWJlLWxvbmc=";

    private final Instant fixedInstant = Instant.parse("2026-06-24T00:00:00Z");
    private final Clock fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC);

    private final JwtProperties jwtProperties = new JwtProperties(
            TEST_JWT_SECRET,
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

    @Test
    @DisplayName("만료된 Access Token을 파싱하면 예외가 발생한다")
    void parseAccessToken_expired() {
        JwtProperties expiredProperties = new JwtProperties(
                TEST_JWT_SECRET,
                Duration.ofSeconds(1),
                "mopl-test"
        );

        JwtProvider expiredJwtProvider = new JwtProvider(expiredProperties, fixedClock);

        String token = expiredJwtProvider.generateAccessToken(
                UUID.randomUUID(),
                UserRole.USER,
                UUID.randomUUID()
        );

        Clock afterExpirationClock = Clock.fixed(
                fixedInstant.plusSeconds(2),
                ZoneOffset.UTC
        );

        JwtProvider afterExpirationJwtProvider = new JwtProvider(expiredProperties, afterExpirationClock);

        assertThatThrownBy(() -> afterExpirationJwtProvider.parseAccessToken(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("잘못된 Access Token을 파싱하면 예외가 발생한다")
    void parseAccessToken_invalid() {
        assertThatThrownBy(() -> jwtProvider.parseAccessToken("invalid-token"))
                .isInstanceOf(JwtException.class);
    }
}
