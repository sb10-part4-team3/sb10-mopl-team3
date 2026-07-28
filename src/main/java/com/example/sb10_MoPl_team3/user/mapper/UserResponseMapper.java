package com.example.sb10_MoPl_team3.user.mapper;

import com.example.sb10_MoPl_team3.global.file.FileStorageService;
import com.example.sb10_MoPl_team3.user.dto.response.UserDto;
import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;
import com.example.sb10_MoPl_team3.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserResponseMapper {

    private final FileStorageService fileStorageService;

    public UserDto toDto(User user) {
        return toDto(UserMapper.toDto(user));
    }

    public UserDto toDto(UserDto dto) {
        return new UserDto(
                dto.id(),
                dto.createdAt(),
                dto.email(),
                dto.name(),
                fileStorageService.toAccessibleUrl(dto.profileImageUrl()),
                dto.role(),
                dto.locked()
        );
    }

    public UserSummary toSummary(User user) {
        return toSummary(UserMapper.toSummary(user));
    }

    public UserSummary toSummary(UserSummary summary) {
        return new UserSummary(
                summary.userId(),
                summary.name(),
                fileStorageService.toAccessibleUrl(summary.profileImageUrl())
        );
    }
}
