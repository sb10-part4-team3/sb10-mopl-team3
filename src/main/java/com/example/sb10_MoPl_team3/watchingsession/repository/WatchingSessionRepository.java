package com.example.sb10_MoPl_team3.watchingsession.repository;

import com.example.sb10_MoPl_team3.watchingsession.entity.WatchingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface WatchingSessionRepository extends JpaRepository<WatchingSession, UUID>,
        WatchingSessionRepositoryCustom {

    Optional<WatchingSession> findByWatcherId(UUID watcherId);

    @Query("select session.content.id from WatchingSession session "
            + "where session.watcher.id = :watcherId")
    Optional<UUID> findContentIdByWatcherId(@Param("watcherId") UUID watcherId);

}
