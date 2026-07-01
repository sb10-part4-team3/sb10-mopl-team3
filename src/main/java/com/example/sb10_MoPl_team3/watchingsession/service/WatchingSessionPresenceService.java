package com.example.sb10_MoPl_team3.watchingsession.service;

import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;
import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionChange;
import com.example.sb10_MoPl_team3.watchingsession.repository.WatchingSessionRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WatchingSessionPresenceService {

    private final WatchingSessionPersistenceService persistenceService;
    private final WatchingSessionRedisRepository redisRepository;

    public List<WatchingSessionChange> join(UUID contentId, UUID watcherId) {
        List<WatchingSessionChange> changes = new ArrayList<>();
        var joinResult = persistenceService.join(contentId, watcherId);
        joinResult.previousContentId().ifPresent(previousContentId -> {
            redisRepository.removeWatcher(previousContentId, watcherId);
            changes.add(snapshot(previousContentId));
        });

        boolean added = redisRepository.addWatcher(contentId, joinResult.watcher());
        if (added) {
            changes.add(snapshot(contentId));
        }
        return List.copyOf(changes);
    }

    public WatchingSessionChange leave(UUID contentId, UUID watcherId) {
        persistenceService.leave(contentId, watcherId);
        redisRepository.removeWatcher(contentId, watcherId);
        return snapshot(contentId);
    }

    private WatchingSessionChange snapshot(UUID contentId) {
        List<UserSummary> watchers = redisRepository.findWatchers(contentId).stream()
                .sorted(Comparator.comparing(UserSummary::name)
                        .thenComparing(UserSummary::userId))
                .toList();
        return new WatchingSessionChange(contentId, watchers);
    }
}
