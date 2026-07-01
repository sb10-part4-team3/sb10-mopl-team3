package com.example.sb10_MoPl_team3.watchingsession.service;

import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.repository.ContentRepository;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.mapper.UserMapper;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionChange;
import com.example.sb10_MoPl_team3.watchingsession.entity.WatchingSession;
import com.example.sb10_MoPl_team3.watchingsession.repository.WatchingSessionRedisRepository;
import com.example.sb10_MoPl_team3.watchingsession.repository.WatchingSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WatchingSessionPresenceService {

    private final WatchingSessionRepository watchingSessionRepository;
    private final WatchingSessionRedisRepository redisRepository;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;

    @Transactional
    public List<WatchingSessionChange> join(UUID contentId, UUID watcherId) {
        User watcher = userRepository.findById(watcherId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));

        List<WatchingSessionChange> changes = new ArrayList<>();
        watchingSessionRepository.findByWatcherId(watcherId).ifPresentOrElse(existing -> {
            UUID previousContentId = existing.getContent().getId();
            if (!previousContentId.equals(contentId)) {
                watchingSessionRepository.delete(existing);
                watchingSessionRepository.flush();
                redisRepository.removeWatcher(previousContentId, watcherId);
                changes.add(snapshot(previousContentId));
                watchingSessionRepository.save(new WatchingSession(watcher, content));
            }
        }, () -> watchingSessionRepository.save(new WatchingSession(watcher, content)));

        redisRepository.addWatcher(contentId, watcherId);
        changes.add(snapshot(contentId));
        return List.copyOf(changes);
    }

    @Transactional
    public WatchingSessionChange leave(UUID contentId, UUID watcherId) {
        watchingSessionRepository.findByWatcherId(watcherId)
                .filter(session -> session.getContent().getId().equals(contentId))
                .ifPresent(watchingSessionRepository::delete);
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
