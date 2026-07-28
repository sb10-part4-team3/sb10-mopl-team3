package com.example.sb10_MoPl_team3.follow.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record FollowRequest(
        @NotNull
        UUID followeeId
) {
}
