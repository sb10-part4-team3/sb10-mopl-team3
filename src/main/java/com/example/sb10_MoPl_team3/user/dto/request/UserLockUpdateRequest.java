package com.example.sb10_MoPl_team3.user.dto.request;

import jakarta.validation.constraints.NotNull;

public record UserLockUpdateRequest(
        @NotNull
        Boolean locked
) {
}
