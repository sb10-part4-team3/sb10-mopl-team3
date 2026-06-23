package com.example.sb10_MoPl_team3.domain.user.mapper;

import com.example.sb10_MoPl_team3.domain.user.dto.response.UserResponse;
import com.example.sb10_MoPl_team3.domain.user.entity.User;

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
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
