package com.example.sb10_MoPl_team3.user.service;

import com.example.sb10_MoPl_team3.user.exception.DuplicatedEmailException;
import com.example.sb10_MoPl_team3.user.dto.request.UserCreateRequest;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.enums.UserStatus;
import com.example.sb10_MoPl_team3.user.event.UserProfileUpdatedEvent;
import com.example.sb10_MoPl_team3.user.event.UserWithdrawnEvent;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.ApplicationEventPublisher;
import com.example.sb10_MoPl_team3.global.file.FileStorageService;
import com.example.sb10_MoPl_team3.global.security.UserAuthorizationService;
import com.example.sb10_MoPl_team3.global.security.exception.AccessDeniedBusinessException;
import com.example.sb10_MoPl_team3.user.dto.request.UserUpdateRequest;
import com.example.sb10_MoPl_team3.user.dto.response.UserDto;
import com.example.sb10_MoPl_team3.user.exception.UserNotFoundException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;
import com.example.sb10_MoPl_team3.auth.entity.AuthSession;
import com.example.sb10_MoPl_team3.auth.repository.AuthSessionRepository;
import com.example.sb10_MoPl_team3.user.dto.request.UserPasswordUpdateRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import java.util.Optional;
import java.util.UUID;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

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

    @Mock
    private AuthSessionRepository authSessionRepository;

    @Mock
    private Clock clock;

    @Mock
    private ApplicationEventPublisher eventPublisher;

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
        ArgumentCaptor<UserProfileUpdatedEvent> eventCaptor =
                ArgumentCaptor.forClass(UserProfileUpdatedEvent.class);
        then(eventPublisher).should().publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().user().name()).isEqualTo("변경된이름");
        assertThat(eventCaptor.getValue().user().profileImageUrl())
                .isEqualTo("https://image.test/old.png");
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

    @Test
    @DisplayName("타인의 프로필 수정은 권한 검증에서 즉시 실패하고 조회와 업로드를 수행하지 않는다")
    void updateUser_forbidden_shortCircuitsBeforeSideEffects() {
        // given
        UUID userId = UUID.randomUUID();
        UserUpdateRequest request = new UserUpdateRequest("변경된이름");
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "profile.png",
                "image/png",
                "image".getBytes()
        );

        willThrow(new AccessDeniedBusinessException())
                .given(userAuthorizationService).validateSelf(userId);

        // when & then
        assertThatThrownBy(() -> userService.updateUser(userId, request, image))
                .isInstanceOf(AccessDeniedBusinessException.class);

        then(userRepository).shouldHaveNoInteractions();
        then(fileStorageService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("프로필 이미지 업로드 후 DB 반영에 실패하면 업로드된 이미지를 삭제한다")
    void updateUser_deleteUploadedImageWhenPersistenceFails() {
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
        willThrow(new RuntimeException("flush failed"))
                .given(userRepository).flush();

        // when & then
        assertThatThrownBy(() -> userService.updateUser(userId, request, image))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("flush failed");

        then(fileStorageService).should().deleteByUrl("https://image.test/new.png");
    }

    @Test
    @DisplayName("프로필 이미지 교체가 성공하면 커밋 후 기존 이미지를 삭제한다")
    void updateUser_deletePreviousImageAfterCommit() {
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

        TransactionSynchronizationManager.initSynchronization();

        try {
            // when
            UserDto response = userService.updateUser(userId, request, image);

            // then
            assertThat(response.profileImageUrl()).isEqualTo("https://image.test/new.png");

            then(fileStorageService).should(never())
                    .deleteByUrl("https://image.test/old.png");

            TransactionSynchronizationUtils.triggerAfterCommit();

            then(fileStorageService).should()
                    .deleteByUrl("https://image.test/old.png");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("본인 계정을 탈퇴하면 상태를 WITHDRAWN으로 변경하고 모든 세션을 무효화한다")
    void withdrawUser_success() {
        // given
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-07-01T00:00:00Z");

        User user = new User(
                "user@test.com",
                "User",
                "encoded-password",
                null,
                UserRole.USER
        );

        AuthSession session1 = AuthSession.create(
                userId,
                "refresh-token-hash-1",
                now.plusSeconds(3600),
                now
        );

        AuthSession session2 = AuthSession.create(
                userId,
                "refresh-token-hash-2",
                now.plusSeconds(3600),
                now
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(authSessionRepository.findAllByUserId(userId)).willReturn(List.of(session1, session2));
        given(clock.instant()).willReturn(now);

        // when
        userService.withdrawUser(userId);

        // then
        then(userAuthorizationService).should().validateSelf(userId);
        then(userRepository).should().findById(userId);
        then(authSessionRepository).should().findAllByUserId(userId);
        then(authSessionRepository).should().saveAll(List.of(session1, session2));
        then(eventPublisher).should().publishEvent(new UserWithdrawnEvent(userId));

        assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);

        assertThat(session1.isRevoked()).isTrue();
        assertThat(session1.getRevokedAt()).isEqualTo(now);
        assertThat(session2.isRevoked()).isTrue();
        assertThat(session2.getRevokedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("존재하지 않는 사용자를 탈퇴하려 하면 예외가 발생한다")
    void withdrawUser_notFound() {
        // given
        UUID userId = UUID.randomUUID();

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.withdrawUser(userId))
                .isInstanceOf(UserNotFoundException.class);

        then(userAuthorizationService).should().validateSelf(userId);
        then(authSessionRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("타인의 계정 탈퇴는 권한 검증에서 즉시 실패하고 조회와 세션 무효화를 수행하지 않는다")
    void withdrawUser_forbidden_shortCircuitsBeforeSideEffects() {
        // given
        UUID userId = UUID.randomUUID();

        willThrow(new AccessDeniedBusinessException())
                .given(userAuthorizationService).validateSelf(userId);

        // when & then
        assertThatThrownBy(() -> userService.withdrawUser(userId))
                .isInstanceOf(AccessDeniedBusinessException.class);

        then(userRepository).shouldHaveNoInteractions();
        then(authSessionRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("현재 비밀번호가 일치하면 새 비밀번호로 변경한다")
    void changePassword_success() {
        // given
        UUID userId = UUID.randomUUID();
        UserPasswordUpdateRequest request =
                new UserPasswordUpdateRequest("newPassword1!");

        User user = new User(
                "user@test.com",
                "User",
                "encoded-current-password",
                null,
                UserRole.USER
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(passwordEncoder.encode(request.password()))
                .willReturn("encoded-new-password");

        // when
        userService.changePassword(userId, request);

        // then
        then(userAuthorizationService).should().validateSelf(userId);
        then(userRepository).should().findById(userId);
        then(passwordEncoder).should()
                .encode(request.password());

        assertThat(user.getPassword()).isEqualTo("encoded-new-password");
    }

    @Test
    @DisplayName("비밀번호 변경 대상 사용자가 존재하지 않으면 예외가 발생한다")
    void changePassword_notFound() {
        // given
        UUID userId = UUID.randomUUID();
        UserPasswordUpdateRequest request =
                new UserPasswordUpdateRequest("newPassword1!");

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.changePassword(userId, request))
                .isInstanceOf(UserNotFoundException.class);

        then(userAuthorizationService).should().validateSelf(userId);
        then(passwordEncoder).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("본인이 아닌 사용자의 비밀번호를 변경하려 하면 예외가 발생하고 조회하지 않는다")
    void changePassword_forbidden_shortCircuitsBeforeSideEffects() {
        // given
        UUID userId = UUID.randomUUID();
        UserPasswordUpdateRequest request =
                new UserPasswordUpdateRequest("newPassword1!");

        willThrow(new AccessDeniedBusinessException())
                .given(userAuthorizationService).validateSelf(userId);

        // when & then
        assertThatThrownBy(() -> userService.changePassword(userId, request))
                .isInstanceOf(AccessDeniedBusinessException.class);

        then(userRepository).shouldHaveNoInteractions();
        then(passwordEncoder).shouldHaveNoInteractions();
    }
}
