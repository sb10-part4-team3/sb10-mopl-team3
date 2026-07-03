package com.example.sb10_MoPl_team3.auth.password.service;

import com.example.sb10_MoPl_team3.auth.password.dto.TemporaryPasswordIssueRequest;
import com.example.sb10_MoPl_team3.auth.password.entity.PasswordResetToken;
import com.example.sb10_MoPl_team3.auth.password.repository.PasswordResetTokenRepository;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
        given(passwordEncoder.encode("temporary1!!"))
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

        then(passwordEncoder).should().encode("temporary1!!");
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
        given(passwordEncoder.encode("temporary1!!"))
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
    }
}