package com.example.sb10_MoPl_team3.auth.password.service;

import com.example.sb10_MoPl_team3.auth.password.dto.TemporaryPasswordIssueRequest;
import com.example.sb10_MoPl_team3.auth.password.entity.PasswordResetToken;
import com.example.sb10_MoPl_team3.auth.password.notification.TemporaryPasswordNotifier;
import com.example.sb10_MoPl_team3.auth.password.repository.PasswordResetTokenRepository;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PasswordResetService {

    private static final Duration TEMPORARY_PASSWORD_EXPIRATION = Duration.ofMinutes(3);

    private final TemporaryPasswordGenerator temporaryPasswordGenerator;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final TemporaryPasswordNotifier temporaryPasswordNotifier;

    @Transactional
    public void issueTemporaryPassword(TemporaryPasswordIssueRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElse(null);

        if (user == null) {
            return;
        }

        Instant now = Instant.now(clock);

        List<PasswordResetToken> existingTokens =
                passwordResetTokenRepository.findAllByUser_IdAndUsedFalse(user.getId());

        if (!existingTokens.isEmpty()) {
            existingTokens.forEach(token -> token.markUsed(now));
            passwordResetTokenRepository.saveAll(existingTokens);
        }

        String temporaryPassword = temporaryPasswordGenerator.generate();

        PasswordResetToken token = PasswordResetToken.create(
                user,
                passwordEncoder.encode(temporaryPassword),
                now.plus(TEMPORARY_PASSWORD_EXPIRATION),
                now
        );

        passwordResetTokenRepository.save(token);
        temporaryPasswordNotifier.send(user.getEmail(), temporaryPassword);
    }

    @Transactional
    public boolean matchesTemporaryPassword(User user, String rawPassword) {
        Instant now = Instant.now(clock);

        List<PasswordResetToken> tokens =
                passwordResetTokenRepository.findAllByUser_IdAndUsedFalseAndExpiresAtAfter(
                        user.getId(),
                        now
                );

        for (PasswordResetToken token : tokens) {
            if (passwordEncoder.matches(rawPassword, token.getTemporaryPassword())) {
                int updatedCount = passwordResetTokenRepository.markUsedIfUsable(token.getId(), now);
                return updatedCount == 1;
            }
        }

        return false;
    }

    @Transactional
    public void discardTemporaryPasswords(User user) {
        Instant now = Instant.now(clock);

        List<PasswordResetToken> tokens =
                passwordResetTokenRepository.findAllByUser_IdAndUsedFalse(user.getId());

        if (tokens.isEmpty()) {
            return;
        }

        tokens.forEach(token -> token.markUsed(now));
        passwordResetTokenRepository.saveAll(tokens);
    }
}
