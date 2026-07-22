package com.example.sb10_MoPl_team3.watchingsession.service;

import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.entity.ContentStats;
import com.example.sb10_MoPl_team3.content.mapper.ContentMapper;
import com.example.sb10_MoPl_team3.content.repository.ContentRepository;
import com.example.sb10_MoPl_team3.content.repository.ContentStatsRepository;
import com.example.sb10_MoPl_team3.content.repository.ContentTagRepository;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import com.example.sb10_MoPl_team3.watchingsession.entity.WatchingSession;
import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionDto;
import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionJoinResult;
import com.example.sb10_MoPl_team3.user.mapper.UserResponseMapper;
import com.example.sb10_MoPl_team3.watchingsession.event.WatchingSessionJoinedEvent;
import com.example.sb10_MoPl_team3.watchingsession.event.WatchingSessionLeftEvent;
import com.example.sb10_MoPl_team3.watchingsession.mapper.WatchingSessionMapper;
import com.example.sb10_MoPl_team3.watchingsession.repository.WatchingSessionRepository;
import com.example.sb10_MoPl_team3.notification.enums.NotificationLevel;
import com.example.sb10_MoPl_team3.notification.event.NotificationAudienceType;
import com.example.sb10_MoPl_team3.notification.event.NotificationFanoutEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

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
    private final ContentTagRepository contentTagRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UserResponseMapper userResponseMapper;

    @Transactional
    public WatchingSessionJoinResult join(UUID contentId, UUID watcherId) {
        User watcher = userRepository.findById(watcherId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));

        Optional<WatchingSession> existing = watchingSessionRepository.findByWatcherId(watcherId);
        if (existing.isEmpty()) {
            WatchingSession watchingSession = watchingSessionRepository.saveAndFlush(
                    new WatchingSession(watcher, content));
            eventPublisher.publishEvent(new WatchingSessionJoinedEvent(contentId));
            publishWatchingActivity(watcher, content);
            return result(Optional.empty(), watchingSession);
        }

        UUID previousContentId = existing.get().getContent().getId();
        if (previousContentId.equals(contentId)) {
            return result(Optional.empty(), existing.get());
        }

        WatchingSessionDto previousWatchingSession = toDto(existing.get());
        watchingSessionRepository.delete(existing.get());
        watchingSessionRepository.flush();
        WatchingSession watchingSession = watchingSessionRepository.saveAndFlush(
                new WatchingSession(watcher, content));
        eventPublisher.publishEvent(new WatchingSessionLeftEvent(previousContentId));
        eventPublisher.publishEvent(new WatchingSessionJoinedEvent(contentId));
        publishWatchingActivity(watcher, content);
        return result(Optional.of(previousWatchingSession), watchingSession);
    }

    @Transactional
    public Optional<WatchingSessionDto> leave(UUID contentId, UUID watcherId) {
        Optional<WatchingSession> watchingSession = watchingSessionRepository.findByWatcherId(watcherId)
                .filter(session -> session.getContent().getId().equals(contentId));
        Optional<WatchingSessionDto> result = watchingSession.map(this::toDto);
        watchingSession.ifPresent(session -> {
            watchingSessionRepository.delete(session);
            eventPublisher.publishEvent(new WatchingSessionLeftEvent(contentId));
        });
        return result;
    }

    private void publishWatchingActivity(User watcher, Content content) {
        eventPublisher.publishEvent(new NotificationFanoutEvent(
                NotificationAudienceType.FOLLOWERS,
                watcher.getId(),
                "시청 시작",
                "%s님이 '%s' 시청을 시작했습니다."
                        .formatted(watcher.getName(), content.getTitle()),
                NotificationLevel.INFO
        ));
    }

    private WatchingSessionJoinResult result(
            Optional<WatchingSessionDto> previousWatchingSession,
            WatchingSession watchingSession
    ) {
        return new WatchingSessionJoinResult(previousWatchingSession, toDto(watchingSession));
    }

    private WatchingSessionDto toDto(WatchingSession watchingSession) {
        Content content = watchingSession.getContent();
        ContentStats stats = contentStatsRepository.findById(content.getId()).orElse(null);
        var tags = contentTagRepository.findTagNamesByContentId(content.getId());
        return WatchingSessionMapper.toDto(
                watchingSession,
                userResponseMapper.toSummary(watchingSession.getWatcher()),
                ContentMapper.toSummary(content, stats, tags)
        );
    }
}
