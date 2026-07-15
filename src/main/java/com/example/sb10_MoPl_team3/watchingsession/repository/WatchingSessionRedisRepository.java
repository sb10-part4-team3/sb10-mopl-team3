package com.example.sb10_MoPl_team3.watchingsession.repository;

import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public interface WatchingSessionRedisRepository {

    record PresenceKey(UUID contentId, UUID watcherId) {
    }

    boolean addWatcher(UUID contentId, UserSummary watcher);

    boolean refreshWatcher(UUID contentId, UUID watcherId);

    Set<PresenceKey> refreshWatchers(Collection<PresenceKey> presences);

    boolean removeWatcher(UUID contentId, UUID watcherId);

    List<UserSummary> removeStaleWatchers(UUID contentId);

    List<UserSummary> findWatchers(UUID contentId);

    long countWatchers(UUID contentId);

    Set<UUID> findActiveContentIds();

    void forEachActiveContentId(Consumer<UUID> consumer);

    void deleteByContentId(UUID contentId);
}
