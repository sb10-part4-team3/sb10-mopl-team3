package com.example.sb10_MoPl_team3.auth.service;

import com.example.sb10_MoPl_team3.auth.dto.AuthTokenResult;
import com.example.sb10_MoPl_team3.auth.dto.request.SignInRequest;
import com.example.sb10_MoPl_team3.auth.dto.response.JwtDto;
import com.example.sb10_MoPl_team3.auth.entity.AuthSession;
import com.example.sb10_MoPl_team3.auth.exception.InvalidCredentialException;
import com.example.sb10_MoPl_team3.auth.exception.InvalidRefreshTokenException;
import com.example.sb10_MoPl_team3.auth.password.service.PasswordResetService;
import com.example.sb10_MoPl_team3.auth.repository.AuthSessionRepository;
import com.example.sb10_MoPl_team3.global.security.AuthUser;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtProperties;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserStatus;
import com.example.sb10_MoPl_team3.user.mapper.UserMapper;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    private final AuthSessionRepository authSessionRepository;
    private final AuthSessionLockManager authSessionLockManager;
    private final JwtProperties jwtProperties;
    private final Clock clock;
    private final PasswordResetService passwordResetService;

    @Transactional
    public AuthTokenResult signin(SignInRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialException::new);

        if (user.getStatus() == UserStatus.LOCKED || user.getStatus() == UserStatus.WITHDRAWN)
            throw new InvalidCredentialException();

        if (!passwordEncoder.matches(request.password(), user.getPassword())
                && !passwordResetService.matchesTemporaryPassword(user, request.password())) {
            throw new InvalidCredentialException();
        }

        return issueTokenForAuthenticatedUser(user);
    }

    @Transactional
    public AuthTokenResult issueTokenForAuthenticatedUser(User user) {
        if (user.getStatus() == UserStatus.LOCKED || user.getStatus() == UserStatus.WITHDRAWN) {
            throw new InvalidCredentialException();
        }

        Instant now = Instant.now(clock);
        revokeExistingSessions(user.getId(), now);

        String refreshToken = tokenService.issueRefreshToken();
        String refreshTokenHash = tokenService.hashRefreshToken(refreshToken);

        AuthSession authSession = AuthSession.create(
                user.getId(),
                refreshTokenHash,
                now.plus(jwtProperties.refreshTokenExpiration()),
                now
        );

        String accessToken = tokenService.issueAccessToken(user, authSession.getId());

        authSessionRepository.save(authSession);

        return new AuthTokenResult(
                new JwtDto(UserMapper.toDto(user), accessToken),
                refreshToken
        );
    }

    @Transactional
    public AuthTokenResult reissueToken(String refreshToken) {
        Instant now = Instant.now(clock);
        String refreshTokenHash = tokenService.hashRefreshToken(refreshToken);

        AuthSession authSession = authSessionRepository.findByRefreshTokenHash(refreshTokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);

        return authSessionLockManager.executeWithLock(authSession.getId(), () -> {
            AuthSession currentSession = authSessionRepository.findById(authSession.getId())
                    .orElseThrow(InvalidRefreshTokenException::new);

            validateRefreshSession(currentSession, refreshTokenHash, now);

            User user = userRepository.findById(currentSession.getUserId())
                    .orElseThrow(InvalidRefreshTokenException::new);

            if (user.getStatus() == UserStatus.LOCKED || user.getStatus() == UserStatus.WITHDRAWN) {
                throw new InvalidRefreshTokenException();
            }

            String newRefreshToken = tokenService.issueRefreshToken();
            String newRefreshTokenHash = tokenService.hashRefreshToken(newRefreshToken);

            currentSession.rotateRefreshToken(
                    newRefreshTokenHash,
                    now.plus(jwtProperties.refreshTokenExpiration()),
                    now
            );

            String accessToken = tokenService.issueAccessToken(user, currentSession.getId());

            authSessionRepository.save(currentSession);

            return new AuthTokenResult(
                    new JwtDto(UserMapper.toDto(user), accessToken),
                    newRefreshToken
            );
        });
    }

    private void validateRefreshSession(AuthSession authSession, String refreshTokenHash, Instant now) {
        if (authSession.isRevoked() || !authSession.getExpiresAt().isAfter(now)) {
            throw new InvalidRefreshTokenException();
        }

        if (!authSession.getRefreshTokenHash().equals(refreshTokenHash)) {
            throw new InvalidRefreshTokenException();
        }
    }

    @Transactional
    public void signout(AuthUser authUser) {
        authSessionLockManager.executeWithLock(authUser.sessionId(), () -> {
            AuthSession authSession = authSessionRepository.findById(authUser.sessionId())
                    .orElse(null);

            if (authSession == null) {
                return null;
            }

            if (!authSession.getUserId().equals(authUser.userId())) {
                throw new InvalidCredentialException();
            }

            authSession.revoke(Instant.now(clock));
            authSessionRepository.save(authSession);

            return null;
        });
    }

    private void revokeExistingSessions(UUID userId, Instant now) {
        Iterable<AuthSession> sessions = authSessionRepository.findAllByUserId(userId);

        for (AuthSession session : sessions) {
            revokeSessionIfOwnedBy(session.getId(), userId, now);
        }
    }

    private void revokeSessionIfOwnedBy(UUID sessionId, UUID userId, Instant now) {
        authSessionLockManager.executeWithLock(sessionId, () -> {
            AuthSession currentSession = authSessionRepository.findById(sessionId)
                    .orElse(null);

            if (currentSession == null || !currentSession.getUserId().equals(userId)) {
                return null;
            }

            currentSession.revoke(now);
            authSessionRepository.save(currentSession);

            return null;
        });
    }
}
