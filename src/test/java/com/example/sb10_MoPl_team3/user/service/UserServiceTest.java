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
import com.example.sb10_MoPl_team3.global.file.FileStorageService;
import com.example.sb10_MoPl_team3.global.security.UserAuthorizationService;
import com.example.sb10_MoPl_team3.user.dto.request.UserUpdateRequest;
import com.example.sb10_MoPl_team3.user.dto.response.UserDto;
import com.example.sb10_MoPl_team3.user.exception.UserNotFoundException;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import java.util.Optional;
import java.util.UUID;
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private UserAuthorizationService userAuthorizationService;

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

    @Test
    @DisplayName("사용자 ID로 사용자 상세 정보를 조회한다")
    void findUser_success() {
        // given
        UUID userId = UUID.randomUUID();
        User user = new User(
                "user@test.com",
                "홍길동",
                "encoded-password",
                "https://image.test/profile.png",
                UserRole.USER
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        UserDto response = userService.findUser(userId);

        // then
        assertThat(response.email()).isEqualTo("user@test.com");
        assertThat(response.name()).isEqualTo("홍길동");
        assertThat(response.profileImageUrl()).isEqualTo("https://image.test/profile.png");
        assertThat(response.role()).isEqualTo(UserRole.USER);
        assertThat(response.locked()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 조회하면 예외가 발생한다")
    void findUser_notFound() {
        // given
        UUID userId = UUID.randomUUID();

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.findUser(userId))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("본인 프로필 이름을 수정한다")
    void updateUser_nameOnly() {
        // given
        UUID userId = UUID.randomUUID();
        UserUpdateRequest request = new UserUpdateRequest("변경된이름");
        User user = new User(
                "user@test.com",
                "기존이름",
                "encoded-password",
                "https://image.test/old.png",
                UserRole.USER
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        UserDto response = userService.updateUser(userId, request, null);

        // then
        then(userAuthorizationService).should().validateSelf(userId);
        then(fileStorageService).should(never()).upload(any());

        assertThat(response.name()).isEqualTo("변경된이름");
        assertThat(response.profileImageUrl()).isEqualTo("https://image.test/old.png");
    }

    @Test
    @DisplayName("본인 프로필 이미지가 있으면 S3에 업로드하고 URL을 저장한다")
    void updateUser_withImage() {
        // given
        UUID userId = UUID.randomUUID();
        UserUpdateRequest request = new UserUpdateRequest("변경된이름");
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "profile.png",
                "image/png",
                "image".getBytes()
        );

        User user = new User(
                "user@test.com",
                "기존이름",
                "encoded-password",
                "https://image.test/old.png",
                UserRole.USER
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(fileStorageService.upload(image)).willReturn("https://image.test/new.png");

        // when
        UserDto response = userService.updateUser(userId, request, image);

        // then
        then(userAuthorizationService).should().validateSelf(userId);
        then(fileStorageService).should().upload(image);

        assertThat(response.name()).isEqualTo("변경된이름");
        assertThat(response.profileImageUrl()).isEqualTo("https://image.test/new.png");
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 프로필을 수정하면 예외가 발생한다")
    void updateUser_notFound() {
        // given
        UUID userId = UUID.randomUUID();
        UserUpdateRequest request = new UserUpdateRequest("변경된이름");

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.updateUser(userId, request, null))
                .isInstanceOf(UserNotFoundException.class);

        then(userAuthorizationService).should().validateSelf(userId);
        then(fileStorageService).should(never()).upload(any());
    }
}