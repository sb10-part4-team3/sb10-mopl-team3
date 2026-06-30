package com.example.sb10_MoPl_team3.user.dto.request;

import com.example.sb10_MoPl_team3.user.enums.UserRole;

import java.util.UUID;

public record UserSearchCondition(
        String emailLike,
        UserRole roleEqual,
        Boolean isLocked,
        String cursor,
        UUID idAfter,
        Integer limit,
        String sortDirection,
        String sortBy
) {
}