package com.example.sb10_MoPl_team3.domain.review.dto;

import com.example.sb10_MoPl_team3.domain.user.dto.response.UserSummaryResponse;

import java.util.UUID;

public record ReviewDto(
        UUID id,
        UUID contentId,
        UserSummaryResponse author,
        String text,
        double rating
) {
}
