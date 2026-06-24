package com.example.sb10_MoPl_team3.auth.service;

import com.example.sb10_MoPl_team3.global.security.jwt.JwtClaims;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtProperties;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtProvider;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtTokenType;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TokenServiceTest {

    private final Instant fixedInstant = Instant.parse("2026-06-24T00:00:00Z");
    private final Clock fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC);

    private final JwtProperties jwtProperties = new JwtProperties(
            "test-secret-key-for-jwt-provider-test-must-be-long-enough",
            Duration.ofHours(1),
            "mopl-test"
    );

    private final JwtProvider jwtProvider = new JwtProvider(jwtProperties, fixedClock);
    private final TokenService tokenService = new TokenService(jwtProvider);

    @Test
    @DisplayName("사용자 정보로 Access Token을 발급한다")
    void issueAccessToken() {
        UUID userId = UUID.randomUUID();
        User user = new User(
                "user@test.com",
                "홍길동",
                "encoded-password",
                null,
                UserRole.USER
        );
        ReflectionTestUtils.setField(user, "id", userId);

        String accessToken = tokenService.issueAccessToken(user);

        JwtClaims claims = jwtProvider.parseAccessToken(accessToken);

        assertThat(claims.userId()).isEqualTo(userId);
        assertThat(claims.role()).isEqualTo(UserRole.USER);
        assertThat(claims.type()).isEqualTo(JwtTokenType.ACCESS);
        assertThat(claims.issuedAt()).isEqualTo(fixedInstant);
        assertThat(claims.expiresAt()).isEqualTo(fixedInstant.plus(Duration.ofHours(1)));
    }
}