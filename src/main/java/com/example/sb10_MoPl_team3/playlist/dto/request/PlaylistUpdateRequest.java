package com.example.sb10_MoPl_team3.playlist.dto.request;

import jakarta.validation.constraints.NotNull;

public record PlaylistUpdateRequest(
        @NotNull
        String title,

        @NotNull
        String description
) {
}
