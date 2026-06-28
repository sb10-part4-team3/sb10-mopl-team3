package com.example.sb10_MoPl_team3.user.bootstrap;

import com.example.sb10_MoPl_team3.user.config.AdminAccountProperties;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.enums.UserStatus;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AdminAccountInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("관리자 계정이 없으면 기본 관리자 계정을 생성한다")
    void run_createAdmin() throws Exception {
        // given
        AdminAccountProperties properties = new AdminAccountProperties(
                "admin@mopl.com",
                "adminPassword1!",
                "Admin"
        );

        AdminAccountInitializer initializer = new AdminAccountInitializer(
                userRepository,
                passwordEncoder,
                properties
        );

        given(userRepository.existsByEmail(properties.email())).willReturn(false);
        given(passwordEncoder.encode(properties.password())).willReturn("encoded-admin-password");

        // when
        initializer.run();

        // then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        then(userRepository).should().save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getEmail()).isEqualTo("admin@mopl.com");
        assertThat(savedUser.getName()).isEqualTo("Admin");
        assertThat(savedUser.getPassword()).isEqualTo("encoded-admin-password");
        assertThat(savedUser.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("관리자 계정이 이미 있으면 새로 생성하지 않는다")
    void run_adminAlreadyExists() throws Exception {
        // given
        AdminAccountProperties properties = new AdminAccountProperties(
                "admin@mopl.com",
                "adminPassword1!",
                "Admin"
        );

        AdminAccountInitializer initializer = new AdminAccountInitializer(
                userRepository,
                passwordEncoder,
                properties
        );

        given(userRepository.existsByEmail(properties.email())).willReturn(true);

        // when
        initializer.run();

        // then
        then(passwordEncoder).should(never()).encode(properties.password());
        then(userRepository).should(never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }
}