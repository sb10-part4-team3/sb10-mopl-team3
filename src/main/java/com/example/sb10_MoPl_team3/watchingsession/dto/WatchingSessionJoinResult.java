package com.example.sb10_MoPl_team3.watchingsession.dto;

import java.util.Optional;

public record WatchingSessionJoinResult(
        Optional<WatchingSessionDto> previousWatchingSession,
        WatchingSessionDto watchingSession
) {
}
