package com.example.sb10_MoPl_team3.watchingsession.dto;

import com.example.sb10_MoPl_team3.content.dto.ContentSummary;
import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;

import java.time.Instant;
import java.util.UUID;

public record WatchingSessionDto(
        UUID id,
        Instant createdAt,
        UserSummary watcher,
        ContentSummary content
) {
}
