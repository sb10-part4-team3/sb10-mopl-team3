package com.example.sb10_MoPl_team3.review.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReviewCreateRequest(
        @NotNull
        UUID contentId,

        @NotNull
        String text,

        @NotNull
        double rating
) {
}
