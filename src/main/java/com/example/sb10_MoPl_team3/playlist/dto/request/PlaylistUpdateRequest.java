package com.example.sb10_MoPl_team3.playlist.dto.request;

import jakarta.annotation.Nullable;

public record PlaylistUpdateRequest(
        @Nullable
        String title,

        @Nullable
        String description
) {
}
