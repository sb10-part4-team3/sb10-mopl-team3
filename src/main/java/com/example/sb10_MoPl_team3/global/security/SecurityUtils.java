package com.example.sb10_MoPl_team3.global.security;

import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    // 로그인 사용자 객체 조회
    public static AuthUser getCurrentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
            || !((authentication.getPrincipal()) instanceof AuthUser authUser)) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIAL);
        }

        return authUser;
    }

    // 로그인 사용자 ID 조회
    public static UUID getCurrentUserId() {
        return getCurrentUser().userId();
    }

    // 로그인 사용자 권한
    public static UserRole getCurrentUserRole() {
        return getCurrentUser().role();
    }

    // 본인 여부 확인
    public static boolean isCurrentUser(UUID userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return getCurrentUserId().equals(userId);
    }
}
