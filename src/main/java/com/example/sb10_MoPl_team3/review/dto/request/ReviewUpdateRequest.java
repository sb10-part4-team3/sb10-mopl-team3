package com.example.sb10_MoPl_team3.review.dto.request;

import jakarta.annotation.Nullable;

public record ReviewUpdateRequest(
        @Nullable
        String text,

        @Nullable
        Double rating
) {
}
