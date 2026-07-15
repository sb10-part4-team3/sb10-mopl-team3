package com.example.sb10_MoPl_team3.watchingsession.repository;

import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface WatchingSessionRedisRepository {

    boolean addWatcher(UUID contentId, UserSummary watcher);

    boolean refreshWatcher(UUID contentId, UUID watcherId);

    boolean removeWatcher(UUID contentId, UUID watcherId);

    List<UserSummary> removeStaleWatchers(UUID contentId);

    List<UserSummary> findWatchers(UUID contentId);

    long countWatchers(UUID contentId);

    Set<UUID> findActiveContentIds();

    void deleteByContentId(UUID contentId);
}
