package com.example.sb10_MoPl_team3.follow.dto;

import java.util.UUID;

public record FollowDto(
        UUID id,
        UUID followeeId,
        UUID followerId
) {
}
