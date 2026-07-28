package com.example.sb10_MoPl_team3.watchingsession.dto;

public record WatchingSessionChange(
        WatchingSessionChangeType type,
        WatchingSessionDto watchingSession,
        long watcherCount
) {
}
