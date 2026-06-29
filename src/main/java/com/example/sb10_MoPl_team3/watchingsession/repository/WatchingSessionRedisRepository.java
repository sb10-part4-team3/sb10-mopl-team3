package com.example.sb10_MoPl_team3.watchingsession.repository;

import java.util.Set;
import java.util.UUID;

public interface WatchingSessionRedisRepository {

    boolean addWatcher(UUID contentId, UUID watcherId);

    boolean removeWatcher(UUID contentId, UUID watcherId);

    Set<UUID> findWatcherIds(UUID contentId);

    long countWatchers(UUID contentId);

    void deleteByContentId(UUID contentId);
}
