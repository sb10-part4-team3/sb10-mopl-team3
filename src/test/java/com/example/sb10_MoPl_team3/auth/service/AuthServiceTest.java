package com.example.sb10_MoPl_team3.auth.service;

import com.example.sb10_MoPl_team3.auth.dto.AuthTokenResult;
import com.example.sb10_MoPl_team3.auth.dto.request.SignInRequest;
import com.example.sb10_MoPl_team3.auth.entity.AuthSession;
import com.example.sb10_MoPl_team3.auth.exception.InvalidCredentialException;
import com.example.sb10_MoPl_team3.auth.exception.InvalidRefreshTokenException;
import com.example.sb10_MoPl_team3.auth.repository.AuthSessionRepository;
import com.example.sb10_MoPl_team3.global.security.AuthUser;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtProperties;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.enums.UserStatus;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Mock
    private AuthSessionRepository authSessionRepository;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private Clock clock;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("이메일과 비밀번호가 일치하면 로그인에 성공하고 토큰과 세션을 생성한다")
    void signIn_success() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-26T00:00:00Z");
        Duration refreshTokenExpiration = Duration.ofDays(7);
        SignInRequest request = new SignInRequest("user@test.com", "password1!");
        User user = new User("user@test.com", "홍길동", "encoded-password", null, UserRole.USER);
        ReflectionTestUtils.setField(user, "id", userId);

        given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(true);
        given(clock.instant()).willReturn(now);
        given(jwtProperties.refreshTokenExpiration()).willReturn(refreshTokenExpiration);
        given(tokenService.issueRefreshToken()).willReturn("refresh-token");
        given(tokenService.hashRefreshToken("refresh-token")).willReturn("refresh-token-hash");
        given(tokenService.issueAccessToken(any(User.class), any(UUID.class))).willReturn("access-token");

        AuthTokenResult response = authService.signIn(request);

        assertThat(response.jwtDto().accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.jwtDto().userDto().email()).isEqualTo("user@test.com");
        assertThat(response.jwtDto().userDto().locked()).isFalse();

        ArgumentCaptor<AuthSession> authSessionCaptor = ArgumentCaptor.forClass(AuthSession.class);
        then(authSessionRepository).should().save(authSessionCaptor.capture());

        AuthSession savedAuthSession = authSessionCaptor.getValue();
        assertThat(savedAuthSession.getId()).isNotNull();
        assertThat(savedAuthSession.getUserId()).isEqualTo(userId);
        assertThat(savedAuthSession.getRefreshTokenHash()).isEqualTo("refresh-token-hash");
        assertThat(savedAuthSession.getExpiresAt()).isEqualTo(now.plus(refreshTokenExpiration));
        assertThat(savedAuthSession.isRevoked()).isFalse();
        assertThat(savedAuthSession.getRevokedAt()).isNull();
        assertThat(savedAuthSession.getCreatedAt()).isEqualTo(now);
        assertThat(savedAuthSession.getTtlSeconds()).isEqualTo(refreshTokenExpiration.toSeconds());

        then(tokenService).should().issueRefreshToken();
        then(tokenService).should().hashRefreshToken("refresh-token");
        then(tokenService).should().issueAccessToken(user, savedAuthSession.getId());
    }

    @Test
    @DisplayName("가입되지 않은 이메일이면 로그인에 실패한다")
    void signIn_notFound() {
        SignInRequest request = new SignInRequest("none@test.com", "password1!");

        given(userRepository.findByEmail(request.email())).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.signIn(request))
                .isInstanceOf(InvalidCredentialException.class);

        then(passwordEncoder).should(never()).matches(any(), any());
        then(tokenService).should(never()).issueRefreshToken();
        then(tokenService).should(never()).issueAccessToken(any(User.class));
        then(tokenService).should(never()).issueAccessToken(any(User.class), any());
        then(authSessionRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 로그인에 실패한다")
    void signIn_wrongPassword() {
        SignInRequest request = new SignInRequest("user@test.com", "wrong-password");
        User user = new User("user@test.com", "홍길동", "encoded-password", null, UserRole.USER);

        given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(false);

        assertThatThrownBy(() -> authService.signIn(request))
                .isInstanceOf(InvalidCredentialException.class);

        then(tokenService).should(never()).issueRefreshToken();
        then(tokenService).should(never()).issueAccessToken(any(User.class));
        then(tokenService).should(never()).issueAccessToken(any(User.class), any());
        then(authSessionRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("잠긴 계정은 로그인할 수 없다")
    void signIn_locked() {
        SignInRequest request = new SignInRequest("user@test.com", "password1!");
        User user = new User("user@test.com", "홍길동", "encoded-password", null, UserRole.USER);
        ReflectionTestUtils.setField(user, "status", UserStatus.LOCKED);

        given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.signIn(request))
                .isInstanceOf(InvalidCredentialException.class);

        then(passwordEncoder).should(never()).matches(any(), any());
        then(tokenService).should(never()).issueRefreshToken();
        then(tokenService).should(never()).issueAccessToken(any(User.class));
        then(tokenService).should(never()).issueAccessToken(any(User.class), any());
        then(authSessionRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("탈퇴한 계정은 로그인할 수 없다")
    void signIn_withdrawn() {
        SignInRequest request = new SignInRequest("user@test.com", "password1!");
        User user = new User("user@test.com", "홍길동", "encoded-password", null, UserRole.USER);
        ReflectionTestUtils.setField(user, "status", UserStatus.WITHDRAWN);

        given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.signIn(request))
                .isInstanceOf(InvalidCredentialException.class);

        then(passwordEncoder).should(never()).matches(any(), any());
        then(tokenService).should(never()).issueRefreshToken();
        then(tokenService).should(never()).issueAccessToken(any(User.class));
        then(tokenService).should(never()).issueAccessToken(any(User.class), any());
        then(authSessionRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("유효한 refresh token이면 새 access token을 발급한다")
    void reissueToken_success() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-28T00:00:00Z");
        String refreshToken = "refresh-token";
        Duration refreshTokenExpiration = Duration.ofDays(7);

        User user = new User(
                "user@test.com",
                "테스트유저",
                "encoded-password",
                null,
                UserRole.USER
        );
        ReflectionTestUtils.setField(user, "id", userId);

        AuthSession authSession = AuthSession.create(
                userId,
                "refresh-token-hash",
                now.plus(Duration.ofDays(7)),
                now
        );

        given(clock.instant()).willReturn(now);
        given(jwtProperties.refreshTokenExpiration()).willReturn(refreshTokenExpiration);
        given(tokenService.hashRefreshToken(refreshToken)).willReturn("refresh-token-hash");
        given(tokenService.issueRefreshToken()).willReturn("new-refresh-token");
        given(tokenService.hashRefreshToken("new-refresh-token")).willReturn("new-refresh-token-hash");
        given(authSessionRepository.findByRefreshTokenHash("refresh-token-hash"))
                .willReturn(Optional.of(authSession));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(tokenService.issueAccessToken(user, authSession.getId())).willReturn("new-access-token");

        AuthTokenResult response = authService.reissueToken(refreshToken);

        assertThat(response.jwtDto().accessToken()).isEqualTo("new-access-token");
        assertThat(response.jwtDto().userDto().email()).isEqualTo("user@test.com");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(authSession.getRefreshTokenHash()).isEqualTo("new-refresh-token-hash");
        assertThat(authSession.getExpiresAt()).isEqualTo(now.plus(refreshTokenExpiration));
        assertThat(authSession.getTtlSeconds()).isEqualTo(refreshTokenExpiration.toSeconds());

        then(tokenService).should().hashRefreshToken("refresh-token");
        then(tokenService).should().issueRefreshToken();
        then(tokenService).should().hashRefreshToken("new-refresh-token");
        then(authSessionRepository).should().findByRefreshTokenHash("refresh-token-hash");
        then(userRepository).should().findById(userId);
        then(authSessionRepository).should().save(authSession);
        then(tokenService).should().issueAccessToken(user, authSession.getId());
    }

    @Test
    @DisplayName("refresh token에 해당하는 세션이 없으면 재발급에 실패한다")
    void reissueToken_sessionNotFound() {
        String refreshToken = "invalid-refresh-token";

        given(tokenService.hashRefreshToken(refreshToken)).willReturn("invalid-refresh-token-hash");
        given(authSessionRepository.findByRefreshTokenHash("invalid-refresh-token-hash"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.reissueToken(refreshToken))
                .isInstanceOf(InvalidRefreshTokenException.class);

        then(userRepository).should(never()).findById(any());
        then(tokenService).should(never()).issueRefreshToken();
        then(tokenService).should(never()).issueAccessToken(any(User.class), any());
    }

    @Test
    @DisplayName("이미 무효화된 세션이면 재발급에 실패한다")
    void reissueToken_revokedSession() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-28T00:00:00Z");
        String refreshToken = "refresh-token";

        AuthSession authSession = AuthSession.create(
                userId,
                "refresh-token-hash",
                now.plus(Duration.ofDays(7)),
                now
        );
        ReflectionTestUtils.setField(authSession, "revoked", true);
        ReflectionTestUtils.setField(authSession, "revokedAt", now);

        given(clock.instant()).willReturn(now);
        given(tokenService.hashRefreshToken(refreshToken)).willReturn("refresh-token-hash");
        given(authSessionRepository.findByRefreshTokenHash("refresh-token-hash"))
                .willReturn(Optional.of(authSession));

        assertThatThrownBy(() -> authService.reissueToken(refreshToken))
                .isInstanceOf(InvalidRefreshTokenException.class);

        then(userRepository).should(never()).findById(any());
        then(tokenService).should(never()).issueRefreshToken();
        then(tokenService).should(never()).issueAccessToken(any(User.class), any());
    }

    @Test
    @DisplayName("만료된 세션이면 재발급에 실패한다")
    void reissueToken_expiredSession() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-28T00:00:00Z");
        String refreshToken = "refresh-token";

        AuthSession authSession = AuthSession.create(
                userId,
                "refresh-token-hash",
                now.plus(Duration.ofDays(7)),
                now
        );
        ReflectionTestUtils.setField(authSession, "expiresAt", now.minusSeconds(1));

        given(clock.instant()).willReturn(now);
        given(tokenService.hashRefreshToken(refreshToken)).willReturn("refresh-token-hash");
        given(authSessionRepository.findByRefreshTokenHash("refresh-token-hash"))
                .willReturn(Optional.of(authSession));

        assertThatThrownBy(() -> authService.reissueToken(refreshToken))
                .isInstanceOf(InvalidRefreshTokenException.class);

        then(userRepository).should(never()).findById(any());
        then(tokenService).should(never()).issueRefreshToken();
        then(tokenService).should(never()).issueAccessToken(any(User.class), any());
    }

    @Test
    @DisplayName("세션의 사용자를 찾을 수 없으면 재발급에 실패한다")
    void reissueToken_userNotFound() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-28T00:00:00Z");
        String refreshToken = "refresh-token";

        AuthSession authSession = AuthSession.create(
                userId,
                "refresh-token-hash",
                now.plus(Duration.ofDays(7)),
                now
        );

        given(clock.instant()).willReturn(now);
        given(tokenService.hashRefreshToken(refreshToken)).willReturn("refresh-token-hash");
        given(authSessionRepository.findByRefreshTokenHash("refresh-token-hash"))
                .willReturn(Optional.of(authSession));
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.reissueToken(refreshToken))
                .isInstanceOf(InvalidRefreshTokenException.class);

        then(tokenService).should(never()).issueRefreshToken();
        then(tokenService).should(never()).issueAccessToken(any(User.class), any());
    }

    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = {"LOCKED", "WITHDRAWN"})
    @DisplayName("잠금 또는 탈퇴 계정은 refresh token으로 토큰을 재발급할 수 없다")
    void reissueToken_invalidUserStatus(UserStatus status) {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-28T00:00:00Z");
        String refreshToken = "refresh-token";

        AuthSession authSession = AuthSession.create(
                userId,
                "refresh-token-hash",
                now.plus(Duration.ofDays(7)),
                now
        );

        User user = new User(
                "user@test.com",
                "Test User",
                "encoded-password",
                null,
                UserRole.USER
        );
        ReflectionTestUtils.setField(user, "id", userId);
        ReflectionTestUtils.setField(user, "status", status);

        given(clock.instant()).willReturn(now);
        given(tokenService.hashRefreshToken(refreshToken)).willReturn("refresh-token-hash");
        given(authSessionRepository.findByRefreshTokenHash("refresh-token-hash"))
                .willReturn(Optional.of(authSession));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.reissueToken(refreshToken))
                .isInstanceOf(InvalidRefreshTokenException.class);

        then(tokenService).should(never()).issueRefreshToken();
        then(tokenService).should(never()).issueAccessToken(any(User.class), any());
        then(authSessionRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("현재 인증 세션이 존재하면 로그아웃 시 세션을 무효화한다")
    void signOut_success() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-28T00:00:00Z");

        AuthUser authUser = new AuthUser(userId, UserRole.USER, sessionId);

        AuthSession authSession = AuthSession.create(
                userId,
                "refresh-token-hash",
                now.plus(Duration.ofDays(7)),
                now
        );
        ReflectionTestUtils.setField(authSession, "id", sessionId);

        given(clock.instant()).willReturn(now);
        given(authSessionRepository.findById(sessionId)).willReturn(Optional.of(authSession));

        authService.signOut(authUser);

        assertThat(authSession.isRevoked()).isTrue();
        assertThat(authSession.getRevokedAt()).isEqualTo(now);

        then(authSessionRepository).should().findById(sessionId);
        then(authSessionRepository).should().save(authSession);
    }

    @Test
    @DisplayName("현재 인증 세션이 이미 없으면 로그아웃은 성공 처리한다")
    void signOut_sessionNotFound() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        AuthUser authUser = new AuthUser(userId, UserRole.USER, sessionId);

        given(authSessionRepository.findById(sessionId)).willReturn(Optional.empty());

        authService.signOut(authUser);

        then(authSessionRepository).should().findById(sessionId);
        then(authSessionRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("현재 인증 정보에 sessionId가 없으면 로그아웃에 실패한다")
    void signOut_sessionIdNull() {
        AuthUser authUser = new AuthUser(UUID.randomUUID(), UserRole.USER, null);

        assertThatThrownBy(() -> authService.signOut(authUser))
                .isInstanceOf(InvalidCredentialException.class);

        then(authSessionRepository).should(never()).findById(any());
        then(authSessionRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("현재 인증 사용자와 세션의 사용자가 다르면 로그아웃에 실패한다")
    void signOut_userMismatch() {
        UUID sessionId = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-28T00:00:00Z");
        AuthUser authUser = new AuthUser(UUID.randomUUID(), UserRole.USER, sessionId);

        AuthSession authSession = AuthSession.create(
                UUID.randomUUID(),
                "refresh-token-hash",
                now.plus(Duration.ofDays(7)),
                now
        );
        ReflectionTestUtils.setField(authSession, "id", sessionId);

        given(authSessionRepository.findById(sessionId)).willReturn(Optional.of(authSession));

        assertThatThrownBy(() -> authService.signOut(authUser))
                .isInstanceOf(InvalidCredentialException.class);

        then(authSessionRepository).should().findById(sessionId);
        then(authSessionRepository).should(never()).save(any());
    }
}
