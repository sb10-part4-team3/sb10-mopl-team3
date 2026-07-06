package com.example.sb10_MoPl_team3.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class AuthSessionLockManager {

    private static final String LOCK_KEY_PREFIX = "auth_session_lock:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(3);

    private final StringRedisTemplate redisTemplate;

    public <T> T executeWithLock(UUID sessionId, Supplier<T> supplier) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(supplier, "supplier must not be null");

        String lockKey = LOCK_KEY_PREFIX + sessionId;
        String lockValue = UUID.randomUUID().toString();

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, LOCK_TTL);

        if (!Boolean.TRUE.equals(acquired)) {
            throw new IllegalStateException("Auth session is locked");
        }

        try {
            return supplier.get();
        } finally {
            releaseLock(lockKey, lockValue);
        }
    }

    public void executeWithLock(UUID sessionId, Runnable runnable) {
        executeWithLock(sessionId, () -> {
            runnable.run();
            return null;
        });
    }

    private void releaseLock(String lockKey, String lockValue) {
        try {
            String currentValue = redisTemplate.opsForValue().get(lockKey);
            if (lockValue.equals(currentValue)) {
                redisTemplate.delete(lockKey);
            }
        } catch (DataAccessException ignored) {
            // Lock TTL will eventually release the key.
        }
    }
}