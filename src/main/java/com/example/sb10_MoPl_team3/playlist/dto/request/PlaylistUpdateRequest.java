package com.example.sb10_MoPl_team3.playlist.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PlaylistUpdateRequest(
        @NotBlank
        String title,

        @NotBlank
        String description
) {
}
