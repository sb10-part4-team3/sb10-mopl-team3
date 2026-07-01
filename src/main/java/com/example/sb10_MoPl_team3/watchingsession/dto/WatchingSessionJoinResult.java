package com.example.sb10_MoPl_team3.watchingsession.dto;

import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;

import java.util.Optional;
import java.util.UUID;

public record WatchingSessionJoinResult(
        Optional<UUID> previousContentId,
        UserSummary watcher
) {
}
