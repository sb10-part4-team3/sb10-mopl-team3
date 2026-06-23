package com.example.sb10_MoPl_team3.domain.user.dto.response;

import com.example.sb10_MoPl_team3.domain.user.enums.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String name,
        String profileImageUrl,
        UserRole role,
        boolean locked,
        Instant createdAt
) {
}
