package com.example.sb10_MoPl_team3.watchingsession.service;

import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionChange;
import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionChangeType;
import com.example.sb10_MoPl_team3.watchingsession.repository.WatchingSessionRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WatchingSessionPresenceService {

    private final WatchingSessionPersistenceService persistenceService;
    private final WatchingSessionRedisRepository redisRepository;
    private final WatchingSessionViewerCountService viewerCountService;

    public List<WatchingSessionChange> join(UUID contentId, UUID watcherId) {
        List<WatchingSessionChange> changes = new ArrayList<>();
        var joinResult = persistenceService.join(contentId, watcherId);
        joinResult.previousWatchingSession().ifPresent(previousWatchingSession -> {
            UUID previousContentId = previousWatchingSession.content().id();
            redisRepository.removeWatcher(previousContentId, watcherId);
            viewerCountService.sync(previousContentId);
            changes.add(new WatchingSessionChange(
                    WatchingSessionChangeType.LEAVE,
                    previousWatchingSession,
                    redisRepository.countWatchers(previousContentId)
            ));
        });

        redisRepository.addWatcher(contentId, joinResult.watchingSession().watcher());
        viewerCountService.sync(contentId);
        changes.add(new WatchingSessionChange(
                WatchingSessionChangeType.JOIN,
                joinResult.watchingSession(),
                redisRepository.countWatchers(contentId)
        ));
        return List.copyOf(changes);
    }

    public List<WatchingSessionChange> leave(UUID contentId, UUID watcherId) {
        var watchingSession = persistenceService.leave(contentId, watcherId);
        redisRepository.removeWatcher(contentId, watcherId);
        viewerCountService.sync(contentId);
        return watchingSession
                .map(session -> List.of(new WatchingSessionChange(
                        WatchingSessionChangeType.LEAVE,
                        session,
                        redisRepository.countWatchers(contentId)
                )))
                .orElseGet(List::of);
    }

    public boolean refresh(UUID contentId, UUID watcherId) {
        return redisRepository.refreshWatcher(contentId, watcherId);
    }

    public List<WatchingSessionChange> removeStaleWatchers(UUID contentId) {
        var staleWatchers = redisRepository.removeStaleWatchers(contentId);
        if (staleWatchers.isEmpty()) {
            viewerCountService.sync(contentId);
            return List.of();
        }

        List<WatchingSessionChange> changes = new ArrayList<>();
        for (var watcher : staleWatchers) {
            persistenceService.leave(contentId, watcher.userId())
                    .ifPresent(session -> changes.add(new WatchingSessionChange(
                            WatchingSessionChangeType.LEAVE,
                            session,
                            redisRepository.countWatchers(contentId)
                    )));
        }
        viewerCountService.sync(contentId);
        return List.copyOf(changes);
    }
}
