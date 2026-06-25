package com.example.sb10_MoPl_team3.review.dto.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.NotBlank;

public record ReviewUpdateRequest(
        @NotBlank
        String text,

        @NotBlank
        Double rating
) {
}
