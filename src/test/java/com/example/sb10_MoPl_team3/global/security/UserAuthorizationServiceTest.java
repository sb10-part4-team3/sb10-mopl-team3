package com.example.sb10_MoPl_team3.global.security;

import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.global.security.exception.AccessDeniedBusinessException;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserAuthorizationServiceTest {

    private final UserAuthorizationService userAuthorizationService =
            new UserAuthorizationService();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("현재 인증 사용자와 대상 userId가 같으면 true를 반환한다")
    void isSelf_true() {
        UUID userId = UUID.randomUUID();
        setAuthentication(userId);

        boolean result = userAuthorizationService.isSelf(userId);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("현재 인증 사용자와 대상 userId가 다르면 false를 반환한다")
    void isSelf_false() {
        setAuthentication(UUID.randomUUID());

        boolean result = userAuthorizationService.isSelf(UUID.randomUUID());

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("현재 인증 사용자와 대상 userId가 같으면 예외가 발생하지 않는다")
    void validateSelf() {
        UUID userId = UUID.randomUUID();
        setAuthentication(userId);

        assertThatCode(() -> userAuthorizationService.validateSelf(userId))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("현재 인증 사용자와 대상 userId가 다르면 접근 거부 예외가 발생한다")
    void validateSelf_notSelf() {
        setAuthentication(UUID.randomUUID());

        assertThatThrownBy(() -> userAuthorizationService.validateSelf(UUID.randomUUID()))
                .isInstanceOfSatisfying(AccessDeniedBusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED)
                );
    }

    @Test
    @DisplayName("인증 정보가 없으면 인증 예외가 발생한다")
    void validateSelf_noAuthentication() {
        assertThatThrownBy(() -> userAuthorizationService.validateSelf(UUID.randomUUID()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIAL)
                );
    }

    private void setAuthentication(UUID userId) {
        AuthUser authUser = new AuthUser(userId, UserRole.USER, null);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        authUser,
                        null,
                        authUser.authorities()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
