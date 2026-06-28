package com.example.sb10_MoPl_team3.auth.service;

import com.example.sb10_MoPl_team3.auth.dto.request.SignInRequest;
import com.example.sb10_MoPl_team3.auth.dto.request.TokenReissueRequest;
import com.example.sb10_MoPl_team3.auth.dto.response.JwtDto;
import com.example.sb10_MoPl_team3.auth.dto.response.TokenResponse;
import com.example.sb10_MoPl_team3.auth.entity.AuthSession;
import com.example.sb10_MoPl_team3.auth.exception.InvalidCredentialException;
import com.example.sb10_MoPl_team3.auth.exception.InvalidRefreshTokenException;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    private final AuthSessionRepository authSessionRepository;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    @Transactional
    public JwtDto signIn(SignInRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialException::new);

        if (user.getStatus() == UserStatus.LOCKED || user.getStatus() == UserStatus.WITHDRAWN)
            throw new InvalidCredentialException();

        if (!passwordEncoder.matches(request.password(), user.getPassword()))
            throw new InvalidCredentialException();

        Instant now = Instant.now(clock);
        String refreshToken = tokenService.issueRefreshToken();
        String refreshTokenHash = tokenService.hashRefreshToken(refreshToken);

        AuthSession authSession = AuthSession.create(
                user.getId(),
                refreshTokenHash,
                now.plus(jwtProperties.refreshTokenExpiration()),
                now
        );

        authSessionRepository.save(authSession);

        String accessToken = tokenService.issueAccessToken(user, authSession.getId());

        return new JwtDto(
                UserMapper.toDto(user),
                accessToken,
                refreshToken
        );
    }

    public TokenResponse reissueAccessToken(TokenReissueRequest request) {
        Instant now = Instant.now(clock);
        String refreshTokenHash = tokenService.hashRefreshToken(request.refreshToken());

        AuthSession authSession = authSessionRepository.findByRefreshTokenHash(refreshTokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (authSession.isRevoked() || !authSession.getExpiresAt().isAfter(now)) {
            throw new InvalidRefreshTokenException();
        }

        User user = userRepository.findById(authSession.getUserId())
                .orElseThrow(InvalidRefreshTokenException::new);

        if (user.getStatus() == UserStatus.LOCKED || user.getStatus() == UserStatus.WITHDRAWN) {
            throw new InvalidRefreshTokenException();
        }

        String accessToken = tokenService.issueAccessToken(user, authSession.getId());

        return new TokenResponse(accessToken);
    }

    @Transactional
    public void signOut(AuthUser authUser) {
        if (authUser.sessionId() == null) {
            throw new InvalidCredentialException();
        }

        AuthSession authSession = authSessionRepository.findById(authUser.sessionId())
                .orElse(null);

        if (authSession == null) {
            return;
        }

        if (!authSession.getUserId().equals(authUser.userId())) {
            throw new InvalidCredentialException();
        }

        authSession.revoke(Instant.now(clock));
        authSessionRepository.save(authSession);
    }
}
