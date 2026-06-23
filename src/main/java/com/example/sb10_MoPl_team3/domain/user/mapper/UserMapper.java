package com.example.sb10_MoPl_team3.domain.user.mapper;

import com.example.sb10_MoPl_team3.domain.user.dto.response.UserDto;
import com.example.sb10_MoPl_team3.domain.user.dto.response.UserSummary;
import com.example.sb10_MoPl_team3.domain.user.entity.User;
import com.example.sb10_MoPl_team3.domain.user.enums.UserStatus;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getCreatedAt(),
                user.getEmail(),
                user.getName(),
                user.getProfileImageUrl(),
                user.getRole(),
                user.getStatus() == UserStatus.LOCKED
        );
    }

    public static UserSummary toSummary(User user) {
        return new UserSummary(
                user.getId(),
                user.getName(),
                user.getProfileImageUrl()
        );
    }
}
