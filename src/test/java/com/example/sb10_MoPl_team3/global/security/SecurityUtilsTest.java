package com.example.sb10_MoPl_team3.global.security;

import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("현재 인증 사용자가 AuthUser이면 AuthUser를 반환한다")
    void getCurrentUser() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        AuthUser authUser = new AuthUser(userId, UserRole.USER, sessionId);

        setAuthentication(authUser);

        AuthUser currentUser = SecurityUtils.getCurrentUser();

        assertThat(currentUser).isEqualTo(authUser);
    }

    @Test
    @DisplayName("현재 인증 사용자의 userId를 반환한다")
    void getCurrentUserId() {
        UUID userId = UUID.randomUUID();
        AuthUser authUser = new AuthUser(userId, UserRole.USER, null);

        setAuthentication(authUser);

        UUID currentUserId = SecurityUtils.getCurrentUserId();

        assertThat(currentUserId).isEqualTo(userId);
    }

    @Test
    @DisplayName("현재 인증 사용자의 role을 반환한다")
    void getCurrentUserRole() {
        AuthUser authUser = new AuthUser(UUID.randomUUID(), UserRole.ADMIN, null);

        setAuthentication(authUser);

        UserRole currentUserRole = SecurityUtils.getCurrentUserRole();

        assertThat(currentUserRole).isEqualTo(UserRole.ADMIN);
    }

    @Test
    @DisplayName("전달한 userId가 현재 인증 사용자와 같으면 true를 반환한다")
    void isCurrentUser_true() {
        UUID userId = UUID.randomUUID();
        AuthUser authUser = new AuthUser(userId, UserRole.USER, null);

        setAuthentication(authUser);

        boolean result = SecurityUtils.isCurrentUser(userId);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("전달한 userId가 현재 인증 사용자와 다르면 false를 반환한다")
    void isCurrentUser_false() {
        AuthUser authUser = new AuthUser(UUID.randomUUID(), UserRole.USER, null);

        setAuthentication(authUser);

        boolean result = SecurityUtils.isCurrentUser(UUID.randomUUID());

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("인증 정보가 없으면 예외가 발생한다")
    void getCurrentUser_noAuthentication() {
        assertThatThrownBy(SecurityUtils::getCurrentUser)
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("principal이 AuthUser가 아니면 예외가 발생한다")
    void getCurrentUser_invalidPrincipal() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "user-id",
                        null,
                        null
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThatThrownBy(SecurityUtils::getCurrentUser)
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("본인 여부 검증 대상 userId가 null이면 예외가 발생한다")
    void isCurrentUser_nullUserId() {
        assertThatThrownBy(() -> SecurityUtils.isCurrentUser(null))
                .isInstanceOf(NullPointerException.class);
    }

    private void setAuthentication(AuthUser authUser) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        authUser,
                        null,
                        authUser.authorities()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}