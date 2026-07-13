package com.example.sb10_MoPl_team3.user.bootstrap;

import com.example.sb10_MoPl_team3.user.config.AdminAccountProperties;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.enums.UserStatus;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminAccountProperties adminAccountProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        userRepository.findByEmail(adminAccountProperties.email())
                .ifPresentOrElse(
                        this::restoreSystemAdmin,
                        this::createSystemAdmin
                );
    }

    private void createSystemAdmin() {
        User admin = new User(
                adminAccountProperties.email(),
                adminAccountProperties.name(),
                passwordEncoder.encode(adminAccountProperties.password()),
                null,
                UserRole.ADMIN
        );

        userRepository.save(admin);
    }

    private void restoreSystemAdmin(User admin) {
        if (admin.getRole() != UserRole.ADMIN) {
            admin.changeRole(UserRole.ADMIN);
        }

        if (admin.getStatus() != UserStatus.ACTIVE) {
            admin.changeStatus(UserStatus.ACTIVE);
        }
    }
}
