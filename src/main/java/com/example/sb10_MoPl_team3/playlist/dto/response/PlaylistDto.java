package com.example.sb10_MoPl_team3.playlist.dto.response;

import com.example.sb10_MoPl_team3.content.dto.ContentSummary;
import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlaylistDto(
        UUID id,
        UserSummary owner,
        String title,
        String description,
        Instant updatedAt,
        Long subscriberCount,
        boolean subscribedByMe,
        List<ContentSummary> contents
) {
}
