package com.example.sb10_MoPl_team3.watchingsession.repository;

import com.example.sb10_MoPl_team3.watchingsession.entity.WatchingSession;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface WatchingSessionRepositoryCustom {

    List<WatchingSession> findByContentAsc(
            UUID contentId, String watcherName, Instant cursor, UUID idAfter, Pageable pageable
    );

    List<WatchingSession> findByContentDesc(
            UUID contentId, String watcherName, Instant cursor, UUID idAfter, Pageable pageable
    );

    long countByContent(UUID contentId, String watcherName);
}
