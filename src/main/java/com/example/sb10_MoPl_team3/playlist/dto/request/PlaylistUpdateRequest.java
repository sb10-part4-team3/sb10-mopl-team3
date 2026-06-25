package com.example.sb10_MoPl_team3.playlist.dto.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

public record PlaylistUpdateRequest(
        @JsonSetter(nulls = Nulls.FAIL)
        String title,

        @JsonSetter(nulls = Nulls.FAIL)
        String description
) {
}
