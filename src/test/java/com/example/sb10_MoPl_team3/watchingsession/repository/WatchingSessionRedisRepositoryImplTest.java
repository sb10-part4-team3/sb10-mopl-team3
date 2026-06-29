package com.example.sb10_MoPl_team3.watchingsession.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WatchingSessionRedisRepositoryImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Test
    @DisplayName("콘텐츠별 현재 시청자 ID를 Redis Set에 추가한다")
    void addWatcher() {
        WatchingSessionRedisRepositoryImpl repository = repository();
        UUID contentId = UUID.randomUUID();
        UUID watcherId = UUID.randomUUID();
        String key = key(contentId);

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.add(key, watcherId.toString())).thenReturn(1L);

        boolean added = repository.addWatcher(contentId, watcherId);

        assertThat(added).isTrue();
    }

    @Test
    @DisplayName("이미 추가된 시청자 ID를 다시 추가하면 false를 반환한다")
    void addDuplicateWatcher() {
        WatchingSessionRedisRepositoryImpl repository = repository();
        UUID contentId = UUID.randomUUID();
        UUID watcherId = UUID.randomUUID();
        String key = key(contentId);

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.add(key, watcherId.toString())).thenReturn(0L);

        boolean added = repository.addWatcher(contentId, watcherId);

        assertThat(added).isFalse();
    }

    @Test
    @DisplayName("콘텐츠별 현재 시청자 ID를 Redis Set에서 제거한다")
    void removeWatcher() {
        WatchingSessionRedisRepositoryImpl repository = repository();
        UUID contentId = UUID.randomUUID();
        UUID watcherId = UUID.randomUUID();
        String key = key(contentId);

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.remove(key, watcherId.toString())).thenReturn(1L);

        boolean removed = repository.removeWatcher(contentId, watcherId);

        assertThat(removed).isTrue();
    }

    @Test
    @DisplayName("콘텐츠별 현재 시청자 ID 목록을 조회한다")
    void findWatcherIds() {
        WatchingSessionRedisRepositoryImpl repository = repository();
        UUID contentId = UUID.randomUUID();
        UUID firstWatcherId = UUID.randomUUID();
        UUID secondWatcherId = UUID.randomUUID();
        String key = key(contentId);

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members(key)).thenReturn(
                Set.of(firstWatcherId.toString(), secondWatcherId.toString())
        );

        Set<UUID> watcherIds = repository.findWatcherIds(contentId);

        assertThat(watcherIds).containsExactlyInAnyOrder(firstWatcherId, secondWatcherId);
    }

    @Test
    @DisplayName("Redis에 시청자 ID 목록이 없으면 빈 Set을 반환한다")
    void findWatcherIdsEmpty() {
        WatchingSessionRedisRepositoryImpl repository = repository();
        UUID contentId = UUID.randomUUID();
        String key = key(contentId);

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members(key)).thenReturn(null);

        Set<UUID> watcherIds = repository.findWatcherIds(contentId);

        assertThat(watcherIds).isEmpty();
    }

    @Test
    @DisplayName("콘텐츠별 현재 시청자 수를 Redis SCARD로 조회한다")
    void countWatchers() {
        WatchingSessionRedisRepositoryImpl repository = repository();
        UUID contentId = UUID.randomUUID();
        String key = key(contentId);

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.size(key)).thenReturn(3L);

        long count = repository.countWatchers(contentId);

        assertThat(count).isEqualTo(3L);
    }

    @Test
    @DisplayName("콘텐츠별 현재 시청자 목록을 삭제한다")
    void deleteByContentId() {
        WatchingSessionRedisRepositoryImpl repository = repository();
        UUID contentId = UUID.randomUUID();

        repository.deleteByContentId(contentId);

        verify(redisTemplate).delete(key(contentId));
    }

    @Test
    @DisplayName("필수 ID가 null이면 예외가 발생한다")
    void nullId() {
        WatchingSessionRedisRepositoryImpl repository = repository();

        assertThatThrownBy(() -> repository.countWatchers(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("id는 필수입니다.");
    }

    private WatchingSessionRedisRepositoryImpl repository() {
        return new WatchingSessionRedisRepositoryImpl(redisTemplate);
    }

    private String key(UUID contentId) {
        return "watching-sessions:contents:%s:watchers".formatted(contentId);
    }
}
