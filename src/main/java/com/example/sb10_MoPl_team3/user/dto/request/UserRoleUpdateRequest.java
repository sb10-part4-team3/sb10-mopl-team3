package com.example.sb10_MoPl_team3.user.dto.request;

import com.example.sb10_MoPl_team3.user.enums.UserRole;
import jakarta.validation.constraints.NotNull;

public record UserRoleUpdateRequest(
        @NotNull
        UserRole role
) {
}