package com.example.sb10_MoPl_team3.domain.review.dto;

import java.util.UUID;

public record ReviewDto(
        UUID id,
        UUID contentId,
        // 후에 UserSummary로 변경 필요
        Object author,
        String text,
        double rating
) {
}
