package com.example.sb10_MoPl_team3.watchingsession.event;

import com.example.sb10_MoPl_team3.user.event.UserProfileUpdatedEvent;
import com.example.sb10_MoPl_team3.user.event.UserWithdrawnEvent;
import com.example.sb10_MoPl_team3.watchingsession.repository.WatchingSessionRedisRepository;
import com.example.sb10_MoPl_team3.watchingsession.repository.WatchingSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserSummaryCacheEventListener {

    private final WatchingSessionRepository watchingSessionRepository;
    private final WatchingSessionRedisRepository redisRepository;

    @Async("watcherCacheExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void update(UserProfileUpdatedEvent event) {
        try {
            watchingSessionRepository.findContentIdByWatcherId(event.user().userId())
                    .ifPresent(contentId -> redisRepository.addWatcher(contentId, event.user()));
        } catch (RuntimeException exception) {
            log.warn("시청자 프로필 Redis 갱신 실패: userId={}", event.user().userId(), exception);
        }
    }

    @Async("watcherCacheExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void remove(UserWithdrawnEvent event) {
        try {
            watchingSessionRepository.findContentIdByWatcherId(event.userId())
                    .ifPresent(contentId -> redisRepository.removeWatcher(contentId, event.userId()));
        } catch (RuntimeException exception) {
            log.warn("탈퇴 사용자 Redis 제거 실패: userId={}", event.userId(), exception);
        }
    }
}
