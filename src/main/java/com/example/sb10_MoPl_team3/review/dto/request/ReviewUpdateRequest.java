package com.example.sb10_MoPl_team3.review.dto.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

public record ReviewUpdateRequest(
        @JsonSetter(nulls = Nulls.FAIL)
        String text,

        @JsonSetter(nulls = Nulls.FAIL)
        Double rating
) {
}
