package com.example.sb10_MoPl_team3.global.security.jwt;

import com.example.sb10_MoPl_team3.auth.entity.AuthSession;
import com.example.sb10_MoPl_team3.auth.repository.AuthSessionRepository;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class JwtSessionValidatorTest {

    private final Instant now = Instant.parse("2026-06-29T00:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);

    @Mock
    private AuthSessionRepository authSessionRepository;

    @Test
    @DisplayName("세션이 유효하면 검증에 성공한다")
    void validate_success() {
        // given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        JwtClaims claims = claims(userId, sessionId);
        AuthSession session = session(sessionId, userId, now.plus(Duration.ofDays(7)));

        given(authSessionRepository.findById(sessionId)).willReturn(Optional.of(session));

        JwtSessionValidator validator = new JwtSessionValidator(authSessionRepository, clock);

        // when
        validator.validate(claims);

        // then
        then(authSessionRepository).should().findById(sessionId);
    }

    @Test
    @DisplayName("세션을 찾을 수 없으면 검증에 실패한다")
    void validate_sessionNotFound() {
        // given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        JwtClaims claims = claims(userId, sessionId);

        given(authSessionRepository.findById(sessionId)).willReturn(Optional.empty());

        JwtSessionValidator validator = new JwtSessionValidator(authSessionRepository, clock);

        // when & then
        assertThatThrownBy(() -> validator.validate(claims))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("세션이 무효화되어 있으면 검증에 실패한다")
    void validate_revokedSession() {
        // given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        JwtClaims claims = claims(userId, sessionId);
        AuthSession session = session(sessionId, userId, now.plus(Duration.ofDays(7)));
        session.revoke(now);

        given(authSessionRepository.findById(sessionId)).willReturn(Optional.of(session));

        JwtSessionValidator validator = new JwtSessionValidator(authSessionRepository, clock);

        // when & then
        assertThatThrownBy(() -> validator.validate(claims))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("세션이 만료되었으면 검증에 실패한다")
    void validate_expiredSession() {
        // given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        JwtClaims claims = claims(userId, sessionId);
        AuthSession session = session(
                sessionId,
                userId,
                now.minus(Duration.ofMinutes(1)),
                now.minus(Duration.ofMinutes(10))
        );

        given(authSessionRepository.findById(sessionId)).willReturn(Optional.of(session));

        JwtSessionValidator validator = new JwtSessionValidator(authSessionRepository, clock);

        // when & then
        assertThatThrownBy(() -> validator.validate(claims))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("토큰 사용자와 세션 사용자가 다르면 검증에 실패한다")
    void validate_userMismatch() {
        // given
        UUID tokenUserId = UUID.randomUUID();
        UUID sessionUserId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        JwtClaims claims = claims(tokenUserId, sessionId);
        AuthSession session = session(sessionId, sessionUserId, now.plus(Duration.ofDays(7)));

        given(authSessionRepository.findById(sessionId)).willReturn(Optional.of(session));

        JwtSessionValidator validator = new JwtSessionValidator(authSessionRepository, clock);

        // when & then
        assertThatThrownBy(() -> validator.validate(claims))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private JwtClaims claims(UUID userId, UUID sessionId) {
        return new JwtClaims(
                userId,
                UserRole.USER,
                JwtTokenType.ACCESS,
                sessionId,
                now.minus(Duration.ofMinutes(1)),
                now.plus(Duration.ofMinutes(10))
        );
    }

    private AuthSession session(UUID sessionId, UUID userId, Instant expiresAt) {
        return session(sessionId, userId, expiresAt, now);
    }

    private AuthSession session(UUID sessionId, UUID userId, Instant expiresAt, Instant createdAt) {
        AuthSession session = AuthSession.create(
                userId,
                "refresh-token-hash",
                expiresAt,
                createdAt
        );
        ReflectionTestUtils.setField(session, "id", sessionId);

        return session;
    }
}