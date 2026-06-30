package com.example.sb10_MoPl_team3.watchingsession.mapper;

import com.example.sb10_MoPl_team3.content.dto.ContentSummary;
import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;
import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionDto;
import com.example.sb10_MoPl_team3.watchingsession.entity.WatchingSession;

public final class WatchingSessionMapper {

    private WatchingSessionMapper() {
    }

    public static WatchingSessionDto toDto(
            WatchingSession watchingSession,
            UserSummary watcher,
            ContentSummary content
    ) {
        return new WatchingSessionDto(
                watchingSession.getId(),
                watchingSession.getCreatedAt(),
                watcher,
                content
        );
    }
}
