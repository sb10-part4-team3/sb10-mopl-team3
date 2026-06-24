package com.example.sb10_MoPl_team3.user.service;

import com.example.sb10_MoPl_team3.user.exception.DuplicatedEmailException;
import com.example.sb10_MoPl_team3.user.dto.request.UserCreateRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("회원가입 성공 시 비밀번호를 암호화하고 기본 권한 USER와 활성 상태로 저장한다")
    void createUser_success() {
        // given
        UserCreateRequest request = new UserCreateRequest("홍길동", "user@test.com", "password1!");

        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(passwordEncoder.encode(request.password())).willReturn("encoded-password");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        var response = userService.createUser(request);

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
    void createUser_duplicateEmail() {
        // given
        UserCreateRequest request = new UserCreateRequest("홍길동", "user@test.com", "password1!");

        given(userRepository.existsByEmail(request.email())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(DuplicatedEmailException.class);

        then(passwordEncoder).should(never()).encode(any());
        then(userRepository).should(never()).save(any());
    }
}