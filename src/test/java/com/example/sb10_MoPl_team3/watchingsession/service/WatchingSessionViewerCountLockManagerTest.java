package com.example.sb10_MoPl_team3.watchingsession.service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class WatchingSessionViewerCountLockManagerTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOperations;

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void executeWithLock_runsRunnableAndReleasesLockWhenAcquired() {
        var lockManager = lockManager();
        UUID contentId = UUID.randomUUID();
        boolean[] executed = {false};
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(
                eq("watching-session:viewer-count-lock:" + contentId),
                anyString(),
                eq(Duration.ofSeconds(5))))
                .willReturn(true);

        boolean acquired = lockManager.executeWithLock(contentId, () -> executed[0] = true);

        assertThat(acquired).isTrue();
        assertThat(executed[0]).isTrue();
        then(redisTemplate).should().execute(
                any(DefaultRedisScript.class),
                eq(List.of("watching-session:viewer-count-lock:" + contentId)),
                anyString());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void executeWithLock_skipsRunnableWhenLockAlreadyExists() {
        var lockManager = lockManager();
        UUID contentId = UUID.randomUUID();
        boolean[] executed = {false};
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(anyString(), anyString(), eq(Duration.ofSeconds(5))))
                .willReturn(false);

        boolean acquired = lockManager.executeWithLock(contentId, () -> executed[0] = true);

        assertThat(acquired).isFalse();
        assertThat(executed[0]).isFalse();
        then(redisTemplate).should(never()).execute(any(DefaultRedisScript.class), any(List.class), anyString());
    }

    private WatchingSessionViewerCountLockManager lockManager() {
        return new WatchingSessionViewerCountLockManager(redisTemplate);
    }
}
