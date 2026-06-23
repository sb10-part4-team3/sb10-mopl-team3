package com.example.sb10_MoPl_team3.domain.user.dto.response;

import java.util.UUID;

public record UserSummaryResponse(
        UUID userId,
        String name,
        String profileImageUrl
) {
}
