package com.example.sb10_MoPl_team3.playlist.dto;

import com.example.sb10_MoPl_team3.content.dto.ContentSummary;
import com.example.sb10_MoPl_team3.domain.user.dto.response.UserSummaryResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlaylistDto(
        UUID id,
        UserSummaryResponse owner,
        String title,
        String description,
        Instant updatedAt,
        Long subscriberCount,
        boolean subscribedByMe,
        List<ContentSummary> contents
) {
}
