package com.example.sb10_MoPl_team3.user.dto.response;

import java.util.UUID;

public record UserSummary(
        UUID userId,
        String name,
        String profileImageUrl
) {
}
