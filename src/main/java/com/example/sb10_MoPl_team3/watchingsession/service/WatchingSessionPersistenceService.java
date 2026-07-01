package com.example.sb10_MoPl_team3.watchingsession.service;

import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.repository.ContentRepository;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import com.example.sb10_MoPl_team3.watchingsession.entity.WatchingSession;
import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionJoinResult;
import com.example.sb10_MoPl_team3.user.mapper.UserMapper;
import com.example.sb10_MoPl_team3.watchingsession.repository.WatchingSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WatchingSessionPersistenceService {

    private final WatchingSessionRepository watchingSessionRepository;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;

    @Transactional
    public WatchingSessionJoinResult join(UUID contentId, UUID watcherId) {
        User watcher = userRepository.findById(watcherId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));

        Optional<WatchingSession> existing = watchingSessionRepository.findByWatcherId(watcherId);
        if (existing.isEmpty()) {
            watchingSessionRepository.save(new WatchingSession(watcher, content));
            return result(Optional.empty(), watcher);
        }

        UUID previousContentId = existing.get().getContent().getId();
        if (previousContentId.equals(contentId)) {
            return result(Optional.empty(), watcher);
        }

        watchingSessionRepository.delete(existing.get());
        watchingSessionRepository.flush();
        watchingSessionRepository.save(new WatchingSession(watcher, content));
        return result(Optional.of(previousContentId), watcher);
    }

    @Transactional
    public void leave(UUID contentId, UUID watcherId) {
        watchingSessionRepository.findByWatcherId(watcherId)
                .filter(session -> session.getContent().getId().equals(contentId))
                .ifPresent(watchingSessionRepository::delete);
    }

    private WatchingSessionJoinResult result(Optional<UUID> previousContentId, User watcher) {
        return new WatchingSessionJoinResult(previousContentId, UserMapper.toSummary(watcher));
    }
}
