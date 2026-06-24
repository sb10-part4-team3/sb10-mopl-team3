package com.example.sb10_MoPl_team3.review.dto;

import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;

import java.util.UUID;

public record ReviewDto(
        UUID id,
        UUID contentId,
        UserSummary author,
        String text,
        double rating
) {
}
