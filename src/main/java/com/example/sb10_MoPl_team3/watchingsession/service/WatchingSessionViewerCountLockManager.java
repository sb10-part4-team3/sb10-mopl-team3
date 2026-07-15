package com.example.sb10_MoPl_team3.watchingsession.service;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WatchingSessionViewerCountLockManager {

    private static final String LOCK_KEY_PREFIX = "watching-session:viewer-count-lock:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(5);
    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public boolean executeWithLock(UUID contentId, Runnable runnable) {
        Objects.requireNonNull(contentId, "contentId는 필수입니다.");
        Objects.requireNonNull(runnable, "runnable은 필수입니다.");

        String lockKey = LOCK_KEY_PREFIX + contentId;
        String token = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, token, LOCK_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            log.debug("시청자 수 보정 락 획득을 건너뜁니다. contentId={}", contentId);
            return false;
        }

        try {
            runnable.run();
            return true;
        } finally {
            releaseLock(lockKey, token);
        }
    }

    private void releaseLock(String lockKey, String token) {
        try {
            redisTemplate.execute(RELEASE_LOCK_SCRIPT, List.of(lockKey), token);
        } catch (DataAccessException exception) {
            log.warn("시청자 수 보정 락 해제에 실패했습니다. lockKey={}", lockKey, exception);
        }
    }
}
