package com.example.sb10_MoPl_team3.domain.user.mapper;

import com.example.sb10_MoPl_team3.domain.user.dto.response.UserResponse;
import com.example.sb10_MoPl_team3.domain.user.dto.response.UserSummaryResponse;
import com.example.sb10_MoPl_team3.domain.user.entity.User;
import com.example.sb10_MoPl_team3.domain.user.enums.UserStatus;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getProfileImageUrl(),
                user.getRole(),
                user.getStatus() == UserStatus.LOCKED,
                user.getCreatedAt()
        );
    }

    public static UserSummaryResponse toSummaryResponse(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getName(),
                user.getProfileImageUrl()
        );
    }
}
