package com.example.sb10_MoPl_team3.watchingsession.service;

import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.repository.ContentRepository;
import com.example.sb10_MoPl_team3.content.repository.ContentStatsRepository;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import com.example.sb10_MoPl_team3.watchingsession.entity.WatchingSession;
import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionJoinResult;
import com.example.sb10_MoPl_team3.user.mapper.UserMapper;
import com.example.sb10_MoPl_team3.watchingsession.repository.WatchingSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class WatchingSessionPersistenceService {

    private final WatchingSessionRepository watchingSessionRepository;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private final ContentStatsRepository contentStatsRepository;

    @Transactional
    public WatchingSessionJoinResult join(UUID contentId, UUID watcherId) {
        User watcher = userRepository.findById(watcherId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));

        Optional<WatchingSession> existing = watchingSessionRepository.findByWatcherId(watcherId);
        if (existing.isEmpty()) {
            watchingSessionRepository.save(new WatchingSession(watcher, content));
            incrementViewerCount(contentId);
            return result(Optional.empty(), watcher);
        }

        UUID previousContentId = existing.get().getContent().getId();
        if (previousContentId.equals(contentId)) {
            return result(Optional.empty(), watcher);
        }

        watchingSessionRepository.delete(existing.get());
        watchingSessionRepository.flush();
        watchingSessionRepository.save(new WatchingSession(watcher, content));
        decrementViewerCount(previousContentId);
        incrementViewerCount(contentId);
        return result(Optional.of(previousContentId), watcher);
    }

    @Transactional
    public void leave(UUID contentId, UUID watcherId) {
        watchingSessionRepository.findByWatcherId(watcherId)
                .filter(session -> session.getContent().getId().equals(contentId))
                .ifPresent(session -> {
                    watchingSessionRepository.delete(session);
                    decrementViewerCount(contentId);
                });
    }

    private void incrementViewerCount(UUID contentId) {
        if (contentStatsRepository.incrementViewerCount(contentId, Instant.now()) != 1) {
            throw new IllegalStateException("콘텐츠 통계가 존재하지 않습니다. contentId=" + contentId);
        }
    }

    private void decrementViewerCount(UUID contentId) {
        if (contentStatsRepository.decrementViewerCount(contentId, Instant.now()) == 1) {
            return;
        }
        if (!contentStatsRepository.existsById(contentId)) {
            throw new IllegalStateException("콘텐츠 통계가 존재하지 않습니다. contentId=" + contentId);
        }
        log.warn("시청 세션은 존재하지만 viewerCount가 이미 0입니다. contentId={}", contentId);
    }

    private WatchingSessionJoinResult result(Optional<UUID> previousContentId, User watcher) {
        return new WatchingSessionJoinResult(previousContentId, UserMapper.toSummary(watcher));
    }
}
