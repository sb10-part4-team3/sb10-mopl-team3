package com.example.sb10_MoPl_team3.domain.auth.service;

import com.example.sb10_MoPl_team3.domain.auth.dto.request.SignInRequest;
import com.example.sb10_MoPl_team3.domain.auth.dto.response.JwtDto;
import com.example.sb10_MoPl_team3.domain.user.dto.request.UserCreateRequest;
import com.example.sb10_MoPl_team3.domain.user.entity.User;
import com.example.sb10_MoPl_team3.domain.user.enums.UserRole;
import com.example.sb10_MoPl_team3.domain.user.enums.UserStatus;
import com.example.sb10_MoPl_team3.domain.user.repository.UserRepository;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    @DisplayName("회원가입 성공 시 비밀번호를 암호화하고 기본 권한 USER와 활성 상태로 저장한다")
    void signUp_success() {
        // given
        UserCreateRequest request = new UserCreateRequest("홍길동", "user@test.com", "password1!");

        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(passwordEncoder.encode(request.password())).willReturn("encoded-password");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        var response = authService.signUp(request);

        // then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        then(userRepository).should().save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getEmail()).isEqualTo("user@test.com");
        assertThat(savedUser.getName()).isEqualTo("홍길동");
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.getPassword()).isNotEqualTo("password1!");
        assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);

        assertThat(response.email()).isEqualTo("user@test.com");
        assertThat(response.name()).isEqualTo("홍길동");
        assertThat(response.role()).isEqualTo(UserRole.USER);
        assertThat(response.locked()).isFalse();
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 회원가입에 실패한다")
    void signUp_duplicateEmail() {
        // given
        UserCreateRequest request = new UserCreateRequest("홍길동", "user@test.com", "password1!");

        given(userRepository.existsByEmail(request.email())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.signUp(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_EMAIL);

        then(passwordEncoder).should(never()).encode(any());
        then(userRepository).should(never()).save(any());
    }

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
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIAL);

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
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIAL);

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
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIAL);

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
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIAL);

        then(passwordEncoder).should(never()).matches(any(), any());
        then(tokenService).should(never()).issueAccessToken(any());
    }
}