package com.example.sb10_MoPl_team3.domain.auth.service;

import com.example.sb10_MoPl_team3.domain.auth.dto.request.SignInRequest;
import com.example.sb10_MoPl_team3.domain.auth.dto.response.JwtDto;
import com.example.sb10_MoPl_team3.domain.auth.exception.InvalidCredentialException;
import com.example.sb10_MoPl_team3.domain.user.entity.User;
import com.example.sb10_MoPl_team3.domain.user.enums.UserRole;
import com.example.sb10_MoPl_team3.domain.user.enums.UserStatus;
import com.example.sb10_MoPl_team3.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

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

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("이메일과 비밀번호가 일치하면 로그인에 성공하고 액세스 토큰을 반환한다")
    void signIn_success() {
        // given
        SignInRequest request = new SignInRequest("user@test.com", "password1!");
        User user = new User("user@test.com", "홍길동", "encoded-password", null, UserRole.USER);

        given(userRepository.findByEmail(request.username())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(true);
        given(tokenService.issueAccessToken(user)).willReturn("access-token");

        // when
        JwtDto response = authService.signIn(request);

        // then
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.userDto().email()).isEqualTo("user@test.com");
        assertThat(response.userDto().locked()).isFalse();

        then(tokenService).should().issueAccessToken(user);
    }

    @Test
    @DisplayName("가입되지 않은 이메일이면 로그인에 실패한다")
    void signIn_notFound() {
        // given
        SignInRequest request = new SignInRequest("none@test.com", "password1!");

        given(userRepository.findByEmail(request.username())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.signIn(request))
                .isInstanceOf(InvalidCredentialException.class);

        then(passwordEncoder).should(never()).matches(any(), any());
        then(tokenService).should(never()).issueAccessToken(any());
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 로그인에 실패한다")
    void signIn_wrongPassword() {
        // given
        SignInRequest request = new SignInRequest("user@test.com", "wrong-password");
        User user = new User("user@test.com", "홍길동", "encoded-password", null, UserRole.USER);

        given(userRepository.findByEmail(request.username())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.signIn(request))
                .isInstanceOf(InvalidCredentialException.class);

        then(tokenService).should(never()).issueAccessToken(any());
    }

    @Test
    @DisplayName("잠긴 계정은 로그인할 수 없다")
    void signIn_locked() {
        // given
        SignInRequest request = new SignInRequest("user@test.com", "password1!");
        User user = new User("user@test.com", "홍길동", "encoded-password", null, UserRole.USER);
        ReflectionTestUtils.setField(user, "status", UserStatus.LOCKED);

        given(userRepository.findByEmail(request.username())).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> authService.signIn(request))
                .isInstanceOf(InvalidCredentialException.class);

        then(passwordEncoder).should(never()).matches(any(), any());
        then(tokenService).should(never()).issueAccessToken(any());
    }

    @Test
    @DisplayName("탈퇴한 계정은 로그인할 수 없다")
    void signIn_withdrawn() {
        // given
        SignInRequest request = new SignInRequest("user@test.com", "password1!");
        User user = new User("user@test.com", "홍길동", "encoded-password", null, UserRole.USER);
        ReflectionTestUtils.setField(user, "status", UserStatus.WITHDRAWN);

        given(userRepository.findByEmail(request.username())).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> authService.signIn(request))
                .isInstanceOf(InvalidCredentialException.class);

        then(passwordEncoder).should(never()).matches(any(), any());
        then(tokenService).should(never()).issueAccessToken(any());
    }
}