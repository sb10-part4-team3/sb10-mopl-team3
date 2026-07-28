package com.example.sb10_MoPl_team3.watchingsession.service;

import com.example.sb10_MoPl_team3.watchingsession.repository.WatchingSessionRedisRepository;
import com.example.sb10_MoPl_team3.watchingsession.websocket.WatchingSessionBroadcastPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "watching-session.presence.cleanup.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class WatchingSessionStalePresenceReaper {

    private final WatchingSessionRedisRepository redisRepository;
    private final WatchingSessionPresenceService presenceService;
    private final WatchingSessionBroadcastPublisher broadcastPublisher;

    @Scheduled(fixedDelayString = "${watching-session.presence.cleanup.interval-ms:30000}")
    public void reap() {
        redisRepository.forEachActiveContentId(contentId -> {
            try {
                presenceService.removeStaleWatchers(contentId)
                        .forEach(broadcastPublisher::publish);
            } catch (RuntimeException exception) {
                log.warn("만료된 시청 세션 정리에 실패했습니다. contentId={}", contentId, exception);
            }
        });
    }
}
