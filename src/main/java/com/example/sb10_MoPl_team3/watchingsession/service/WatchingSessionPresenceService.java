package com.example.sb10_MoPl_team3.watchingsession.service;

import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;
import com.example.sb10_MoPl_team3.user.mapper.UserMapper;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionChange;
import com.example.sb10_MoPl_team3.watchingsession.repository.WatchingSessionRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WatchingSessionPresenceService {

    private final WatchingSessionPersistenceService persistenceService;
    private final WatchingSessionRedisRepository redisRepository;
    private final UserRepository userRepository;

    public List<WatchingSessionChange> join(UUID contentId, UUID watcherId) {
        List<WatchingSessionChange> changes = new ArrayList<>();
        persistenceService.join(contentId, watcherId).ifPresent(previousContentId -> {
            redisRepository.removeWatcher(previousContentId, watcherId);
            changes.add(snapshot(previousContentId));
        });

        boolean added = redisRepository.addWatcher(contentId, watcherId);
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
        Set<UUID> watcherIds = redisRepository.findWatcherIds(contentId);
        List<UserSummary> watchers = userRepository.findAllById(watcherIds).stream()
                .map(UserMapper::toSummary)
                .sorted(Comparator.comparing(UserSummary::name)
                        .thenComparing(UserSummary::userId))
                .toList();
        return new WatchingSessionChange(contentId, watchers);
    }
}
