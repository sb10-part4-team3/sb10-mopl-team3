package com.example.sb10_MoPl_team3.auth.password.service;

import com.example.sb10_MoPl_team3.auth.password.dto.TemporaryPasswordIssueRequest;
import com.example.sb10_MoPl_team3.auth.password.entity.PasswordResetToken;
import com.example.sb10_MoPl_team3.auth.password.event.TemporaryPasswordIssuedEvent;
import com.example.sb10_MoPl_team3.auth.password.repository.PasswordResetTokenRepository;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.enums.UserStatus;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Clock clock;

    @Mock
    private TemporaryPasswordGenerator temporaryPasswordGenerator;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @Test
    @DisplayName("이메일이 존재하면 고정 임시 비밀번호를 암호화하여 3분 만료 토큰으로 저장한다")
    void issue_success() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-28T00:00:00Z");

        TemporaryPasswordIssueRequest request =
                new TemporaryPasswordIssueRequest("user@test.com");

        User user = new User(
                "user@test.com",
                "User",
                "encoded-password",
                null,
                UserRole.USER
        );
        ReflectionTestUtils.setField(user, "id", userId);

        given(userRepository.findByEmail(request.email()))
                .willReturn(Optional.of(user));
        given(passwordResetTokenRepository.findAllByUser_IdAndUsedFalse(userId))
                .willReturn(List.of());
        given(temporaryPasswordGenerator.generate())
                .willReturn("random-temporary-password");
        given(passwordEncoder.encode("random-temporary-password"))
                .willReturn("encoded-temporary-password");
        given(clock.instant())
                .willReturn(now);

        passwordResetService.issueTemporaryPassword(request);

        ArgumentCaptor<PasswordResetToken> tokenCaptor =
                ArgumentCaptor.forClass(PasswordResetToken.class);

        then(passwordResetTokenRepository).should().save(tokenCaptor.capture());

        PasswordResetToken savedToken = tokenCaptor.getValue();

        assertThat(savedToken.getUser()).isEqualTo(user);
        assertThat(savedToken.getTemporaryPassword())
                .isEqualTo("encoded-temporary-password");
        assertThat(savedToken.getExpiresAt())
                .isEqualTo(now.plus(Duration.ofMinutes(3)));
        assertThat(savedToken.isUsed()).isFalse();
        assertThat(savedToken.getUsedAt()).isNull();

        then(temporaryPasswordGenerator).should().generate();
        then(passwordEncoder).should().encode("random-temporary-password");

        ArgumentCaptor<TemporaryPasswordIssuedEvent> eventCaptor =
                ArgumentCaptor.forClass(TemporaryPasswordIssuedEvent.class);

        then(eventPublisher).should().publishEvent(eventCaptor.capture());

        TemporaryPasswordIssuedEvent event = eventCaptor.getValue();

        assertThat(event.email()).isEqualTo("user@test.com");
        assertThat(event.temporaryPassword()).isEqualTo("random-temporary-password");
    }

    @Test
    @DisplayName("기존 미사용 임시 비밀번호가 있으면 사용 처리한 뒤 새 임시 비밀번호를 저장한다")
    void issue_marksExistingTokensUsed() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-28T00:00:00Z");

        TemporaryPasswordIssueRequest request =
                new TemporaryPasswordIssueRequest("user@test.com");

        User user = new User(
                "user@test.com",
                "User",
                "encoded-password",
                null,
                UserRole.USER
        );
        ReflectionTestUtils.setField(user, "id", userId);

        PasswordResetToken existingToken = PasswordResetToken.create(
                user,
                "old-temporary-password",
                now.plus(Duration.ofMinutes(1)),
                now.minus(Duration.ofMinutes(1))
        );

        given(userRepository.findByEmail(request.email()))
                .willReturn(Optional.of(user));
        given(passwordResetTokenRepository.findAllByUser_IdAndUsedFalse(userId))
                .willReturn(List.of(existingToken));
        given(temporaryPasswordGenerator.generate())
                .willReturn("random-temporary-password");
        given(passwordEncoder.encode("random-temporary-password"))
                .willReturn("encoded-temporary-password");
        given(clock.instant())
                .willReturn(now);

        passwordResetService.issueTemporaryPassword(request);

        assertThat(existingToken.isUsed()).isTrue();
        assertThat(existingToken.getUsedAt()).isEqualTo(now);

        then(passwordResetTokenRepository).should()
                .saveAll(List.of(existingToken));
        then(passwordResetTokenRepository).should()
                .save(any(PasswordResetToken.class));
        then(temporaryPasswordGenerator).should().generate();
        then(passwordEncoder).should().encode("random-temporary-password");
        ArgumentCaptor<TemporaryPasswordIssuedEvent> eventCaptor =
                ArgumentCaptor.forClass(TemporaryPasswordIssuedEvent.class);

        then(eventPublisher).should().publishEvent(eventCaptor.capture());

        TemporaryPasswordIssuedEvent event = eventCaptor.getValue();

        assertThat(event.email()).isEqualTo("user@test.com");
        assertThat(event.temporaryPassword()).isEqualTo("random-temporary-password");

    }

    @Test
    @DisplayName("존재하지 않는 이메일이면 임시 비밀번호를 저장하지 않는다")
    void issue_userNotFound() {
        TemporaryPasswordIssueRequest request =
                new TemporaryPasswordIssueRequest("unknown@test.com");

        given(userRepository.findByEmail(request.email()))
                .willReturn(Optional.empty());

        passwordResetService.issueTemporaryPassword(request);

        then(passwordResetTokenRepository).shouldHaveNoInteractions();
        then(passwordEncoder).should(never()).encode(any());

        then(temporaryPasswordGenerator).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("탈퇴한 사용자에게는 임시 비밀번호를 발급하지 않는다")
    void issue_withdrawnUser() {
        // given
        TemporaryPasswordIssueRequest request =
                new TemporaryPasswordIssueRequest("withdrawn@test.com");

        User user = new User(
                "withdrawn@test.com",
                "Withdrawn User",
                "encoded-password",
                null,
                UserRole.USER
        );
        user.changeStatus(UserStatus.WITHDRAWN);

        given(userRepository.findByEmail(request.email()))
                .willReturn(Optional.of(user));

        // when
        passwordResetService.issueTemporaryPassword(request);

        // then
        then(passwordResetTokenRepository).shouldHaveNoInteractions();
        then(passwordEncoder).should(never()).encode(any());
        then(temporaryPasswordGenerator).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("유효한 임시 비밀번호가 일치하면 true를 반환한다")
    void matchesTemporaryPassword_success() {
        UUID userId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-28T00:00:00Z");

        User user = new User(
                "user@test.com",
                "User",
                "encoded-password",
                null,
                UserRole.USER
        );
        ReflectionTestUtils.setField(user, "id", userId);

        PasswordResetToken token = PasswordResetToken.create(
                user,
                "encoded-temporary-password",
                now.plus(Duration.ofMinutes(1)),
                now.minus(Duration.ofMinutes(1))
        );

        ReflectionTestUtils.setField(token, "id", tokenId);

        given(clock.instant()).willReturn(now);
        given(passwordResetTokenRepository.findAllByUser_IdAndUsedFalseAndExpiresAtAfter(userId, now))
                .willReturn(List.of(token));
        given(passwordEncoder.matches("temporary-password", "encoded-temporary-password"))
                .willReturn(true);

        given(passwordResetTokenRepository.markUsedIfUsable(tokenId, now))
                .willReturn(1);

        boolean result = passwordResetService.matchesTemporaryPassword(user, "temporary-password");

        assertThat(result).isTrue();

        then(passwordResetTokenRepository).should()
                .markUsedIfUsable(tokenId, now);
        then(passwordResetTokenRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("유효한 임시 비밀번호가 없으면 false를 반환한다")
    void matchesTemporaryPassword_noUsableToken() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-28T00:00:00Z");

        User user = new User(
                "user@test.com",
                "User",
                "encoded-password",
                null,
                UserRole.USER
        );
        ReflectionTestUtils.setField(user, "id", userId);

        given(clock.instant()).willReturn(now);
        given(passwordResetTokenRepository.findAllByUser_IdAndUsedFalseAndExpiresAtAfter(userId, now))
                .willReturn(List.of());

        boolean result = passwordResetService.matchesTemporaryPassword(user, "temporary-password");

        assertThat(result).isFalse();

        then(passwordEncoder).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("임시 비밀번호가 일치하지 않으면 false를 반환하고 토큰을 사용 처리하지 않는다")
    void matchesTemporaryPassword_mismatch() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-28T00:00:00Z");

        User user = new User(
                "user@test.com",
                "User",
                "encoded-password",
                null,
                UserRole.USER
        );
        ReflectionTestUtils.setField(user, "id", userId);

        PasswordResetToken token = PasswordResetToken.create(
                user,
                "encoded-temporary-password",
                now.plus(Duration.ofMinutes(1)),
                now.minus(Duration.ofMinutes(1))
        );

        given(clock.instant()).willReturn(now);
        given(passwordResetTokenRepository.findAllByUser_IdAndUsedFalseAndExpiresAtAfter(userId, now))
                .willReturn(List.of(token));
        given(passwordEncoder.matches("wrong-password", "encoded-temporary-password"))
                .willReturn(false);

        boolean result = passwordResetService.matchesTemporaryPassword(user, "wrong-password");

        assertThat(result).isFalse();
        assertThat(token.isUsed()).isFalse();
        assertThat(token.getUsedAt()).isNull();
        then(passwordResetTokenRepository).should(never())
                .markUsedIfUsable(any(), any());

        then(passwordResetTokenRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("임시 비밀번호가 일치해도 이미 다른 요청에서 사용 처리되었으면 실패한다")
    void matchesTemporaryPassword_alreadyConsumedConcurrently() {
        UUID userId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-28T00:00:00Z");

        User user = new User(
                "user@test.com",
                "User",
                "encoded-password",
                null,
                UserRole.USER
        );
        ReflectionTestUtils.setField(user, "id", userId);

        PasswordResetToken token = PasswordResetToken.create(
                user,
                "encoded-temporary-password",
                now.plus(Duration.ofMinutes(3)),
                now.minus(Duration.ofMinutes(1))
        );
        ReflectionTestUtils.setField(token, "id", tokenId);

        given(clock.instant()).willReturn(now);
        given(passwordResetTokenRepository.findAllByUser_IdAndUsedFalseAndExpiresAtAfter(userId, now))
                .willReturn(List.of(token));
        given(passwordEncoder.matches("temporary-password", "encoded-temporary-password"))
                .willReturn(true);
        given(passwordResetTokenRepository.markUsedIfUsable(tokenId, now))
                .willReturn(0);

        boolean result = passwordResetService.matchesTemporaryPassword(user, "temporary-password");

        assertThat(result).isFalse();

        then(passwordResetTokenRepository).should()
                .markUsedIfUsable(tokenId, now);
        then(passwordResetTokenRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("사용자의 미사용 임시 비밀번호를 모두 사용 처리한다")
    void discardTemporaryPasswords_success() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-28T00:00:00Z");

        User user = new User(
                "user@test.com",
                "User",
                "encoded-password",
                null,
                UserRole.USER
        );
        ReflectionTestUtils.setField(user, "id", userId);

        PasswordResetToken token1 = PasswordResetToken.create(
                user,
                "encoded-temporary-password-1",
                now.plus(Duration.ofMinutes(1)),
                now.minus(Duration.ofMinutes(1))
        );
        PasswordResetToken token2 = PasswordResetToken.create(
                user,
                "encoded-temporary-password-2",
                now.plus(Duration.ofMinutes(2)),
                now.minus(Duration.ofMinutes(1))
        );

        given(clock.instant()).willReturn(now);
        given(passwordResetTokenRepository.findAllByUser_IdAndUsedFalse(userId))
                .willReturn(List.of(token1, token2));

        passwordResetService.discardTemporaryPasswords(user);

        assertThat(token1.isUsed()).isTrue();
        assertThat(token1.getUsedAt()).isEqualTo(now);
        assertThat(token2.isUsed()).isTrue();
        assertThat(token2.getUsedAt()).isEqualTo(now);

        then(passwordResetTokenRepository).should().saveAll(List.of(token1, token2));
    }

    @Test
    @DisplayName("사용자의 미사용 임시 비밀번호가 없으면 저장하지 않는다")
    void discardTemporaryPasswords_noTokens() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-28T00:00:00Z");

        User user = new User(
                "user@test.com",
                "User",
                "encoded-password",
                null,
                UserRole.USER
        );
        ReflectionTestUtils.setField(user, "id", userId);

        given(clock.instant()).willReturn(now);
        given(passwordResetTokenRepository.findAllByUser_IdAndUsedFalse(userId))
                .willReturn(List.of());

        passwordResetService.discardTemporaryPasswords(user);

        then(passwordResetTokenRepository).should(never()).saveAll(any());
    }

    @Test
    @DisplayName("만료된 임시 비밀번호는 검증 대상에서 제외되어 false를 반환한다")
    void matchesTemporaryPassword_expiredToken() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-28T00:03:01Z");

        User user = new User(
                "user@test.com",
                "User",
                "encoded-password",
                null,
                UserRole.USER
        );
        ReflectionTestUtils.setField(user, "id", userId);

        given(clock.instant()).willReturn(now);
        given(passwordResetTokenRepository.findAllByUser_IdAndUsedFalseAndExpiresAtAfter(userId, now))
                .willReturn(List.of());

        boolean result = passwordResetService.matchesTemporaryPassword(user, "temporary-password");

        assertThat(result).isFalse();

        then(passwordResetTokenRepository).should()
                .findAllByUser_IdAndUsedFalseAndExpiresAtAfter(userId, now);
        then(passwordEncoder).shouldHaveNoInteractions();
    }
}
