package com.example.sb10_MoPl_team3.global.security;

import com.example.sb10_MoPl_team3.user.enums.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(MethodSecurityTest.TestMethodSecurityService.class)
@ActiveProfiles("test")
class MethodSecurityTest {

    @Autowired
    private TestMethodSecurityService testMethodSecurityService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("ADMIN 권한이면 관리자 메서드를 호출할 수 있다")
    void adminCanAccessAdminMethod() {
        setAuthentication(UserRole.ADMIN);

        String result = testMethodSecurityService.adminOnly();

        assertThat(result).isEqualTo("ok");
    }

    @Test
    @DisplayName("USER 권한이면 관리자 메서드 호출 시 예외가 발생한다")
    void userCannotAccessAdminMethod() {
        setAuthentication(UserRole.USER);

        assertThatThrownBy(() -> testMethodSecurityService.adminOnly())
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("인증 정보가 없으면 관리자 메서드 호출 시 예외가 발생한다")
    void anonymousCannotAccessAdminMethod() {
        assertThatThrownBy(() -> testMethodSecurityService.adminOnly())
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    private void setAuthentication(UserRole role) {
        AuthUser authUser = new AuthUser(UUID.randomUUID(), role, null);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        authUser,
                        null,
                        authUser.authorities()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Service
    public static class TestMethodSecurityService {

        @PreAuthorize("hasRole('ADMIN')")
        public String adminOnly() {
            return "ok";
        }
    }
}