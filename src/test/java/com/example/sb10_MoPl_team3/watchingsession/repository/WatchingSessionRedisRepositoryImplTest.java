package com.example.sb10_MoPl_team3.watchingsession.repository;

import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisSetCommands;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WatchingSessionRedisRepositoryImplTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock HashOperations<String, Object, Object> hashOperations;
    @Mock ValueOperations<String, String> valueOperations;
    @Mock SetOperations<String, String> setOperations;
    @Mock RedisConnection redisConnection;
    @Mock RedisStringCommands stringCommands;
    @Mock RedisSetCommands setCommands;

    @Test
    void addWatcher_storesUserSummaryInHash() throws Exception {
        var repository = repository();
        UUID contentId = UUID.randomUUID();
        UserSummary watcher = new UserSummary(UUID.randomUUID(), "홍길동", "profile");
        String json = new ObjectMapper().writeValueAsString(watcher);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(hashOperations.putIfAbsent(key(contentId), watcher.userId().toString(), json))
                .thenReturn(true);

        assertThat(repository.addWatcher(contentId, watcher)).isTrue();
        verify(valueOperations).set(heartbeatKey(contentId, watcher.userId()), "1", Duration.ofSeconds(30));
        verify(setOperations).add("watching-sessions:active-contents", contentId.toString());
    }

    @Test
    void addWatcher_refreshesSummaryWithoutReportingListChange() throws Exception {
        var repository = repository();
        UUID contentId = UUID.randomUUID();
        UserSummary watcher = new UserSummary(UUID.randomUUID(), "변경된 이름", null);
        String json = new ObjectMapper().writeValueAsString(watcher);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(hashOperations.putIfAbsent(key(contentId), watcher.userId().toString(), json))
                .thenReturn(false);

        assertThat(repository.addWatcher(contentId, watcher)).isFalse();
        verify(hashOperations).put(key(contentId), watcher.userId().toString(), json);
    }

    @Test
    void removeWatcher_deletesHashField() {
        var repository = repository();
        UUID contentId = UUID.randomUUID();
        UUID watcherId = UUID.randomUUID();
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.delete(key(contentId), watcherId.toString())).thenReturn(1L);
        when(hashOperations.size(key(contentId))).thenReturn(0L);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        assertThat(repository.removeWatcher(contentId, watcherId)).isTrue();
        verify(redisTemplate).delete(heartbeatKey(contentId, watcherId));
        verify(setOperations).remove("watching-sessions:active-contents", contentId.toString());
    }

    @Test
    void findWatchers_deserializesSummariesAndIgnoresCorruptedValues() throws Exception {
        var repository = repository();
        UUID contentId = UUID.randomUUID();
        UserSummary watcher = new UserSummary(UUID.randomUUID(), "홍길동", null);
        String json = new ObjectMapper().writeValueAsString(watcher);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(key(contentId))).thenReturn(Map.of(
                watcher.userId().toString(), json,
                "corrupted-user", "invalid-json"
        ));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.multiGet(any()))
                .thenReturn(List.of("1"));
        when(hashOperations.values(key(contentId))).thenReturn(List.of(json, "invalid-json"));
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        assertThat(repository.findWatchers(contentId)).containsExactly(watcher);
    }

    @Test
    void countWatchers_usesHashSize() {
        var repository = repository();
        UUID contentId = UUID.randomUUID();
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(key(contentId))).thenReturn(Map.of());
        when(hashOperations.size(key(contentId))).thenReturn(3L);

        assertThat(repository.countWatchers(contentId)).isEqualTo(3L);
    }

    @Test
    void countWatchers_returnsZeroWhenHashSizeIsNull() {
        var repository = repository();
        UUID contentId = UUID.randomUUID();
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(key(contentId))).thenReturn(Map.of());
        when(hashOperations.size(key(contentId))).thenAnswer(invocation -> null);

        assertThat(repository.countWatchers(contentId)).isZero();
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void removeStaleWatchers_removesExpiredHeartbeatFieldsInBatch() throws Exception {
        var repository = repository();
        UUID contentId = UUID.randomUUID();
        UserSummary alive = new UserSummary(UUID.randomUUID(), "유지", null);
        UserSummary stale = new UserSummary(UUID.randomUUID(), "만료", null);
        ObjectMapper mapper = new ObjectMapper();
        String aliveJson = mapper.writeValueAsString(alive);
        String staleJson = mapper.writeValueAsString(stale);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(hashOperations.entries(key(contentId))).thenReturn(Map.of(
                alive.userId().toString(), aliveJson,
                stale.userId().toString(), staleJson
        ));
        when(valueOperations.multiGet(any())).thenAnswer(invocation -> {
            List<String> keys = invocation.getArgument(0);
            return keys.stream()
                    .map(key -> key.equals(heartbeatKey(contentId, alive.userId())) ? "1" : null)
                    .toList();
        });
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                eq(List.of(key(contentId))),
                any(Object[].class)))
                .thenReturn(List.of(stale.userId().toString()));
        when(hashOperations.size(key(contentId))).thenReturn(1L);

        assertThat(repository.removeStaleWatchers(contentId)).containsExactly(stale);
        verify(redisTemplate).execute(
                any(DefaultRedisScript.class),
                eq(List.of(key(contentId))),
                any(Object[].class));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void removeStaleWatchers_doesNotReportWatcherWhenHeartbeatIsRefreshedBeforeAtomicDelete() throws Exception {
        var repository = repository();
        UUID contentId = UUID.randomUUID();
        UserSummary revived = new UserSummary(UUID.randomUUID(), "재연결", null);
        String json = new ObjectMapper().writeValueAsString(revived);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(hashOperations.entries(key(contentId))).thenReturn(Map.of(revived.userId().toString(), json));
        when(valueOperations.multiGet(any())).thenReturn(Collections.singletonList(null));
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                eq(List.of(key(contentId))),
                any(Object[].class)))
                .thenReturn(List.of());
        when(hashOperations.size(key(contentId))).thenReturn(1L);

        assertThat(repository.removeStaleWatchers(contentId)).isEmpty();
    }

    @Test
    void refreshWatchers_refreshesExistingWatchersAndReturnsMissingOnes() {
        var repository = repository();
        UUID contentId = UUID.randomUUID();
        UUID existingWatcherId = UUID.randomUUID();
        UUID missingWatcherId = UUID.randomUUID();
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.multiGet(
                ArgumentMatchers.eq(key(contentId)),
                ArgumentMatchers.eq(List.of(existingWatcherId.toString(), missingWatcherId.toString()))))
                .thenReturn(Arrays.asList("{}", null));
        when(redisTemplate.getStringSerializer()).thenReturn(StringRedisSerializer.UTF_8);
        when(redisConnection.stringCommands()).thenReturn(stringCommands);
        when(redisConnection.setCommands()).thenReturn(setCommands);
        when(redisTemplate.executePipelined(any(RedisCallback.class))).thenAnswer(invocation -> {
            RedisCallback<?> callback = invocation.getArgument(0);
            callback.doInRedis(redisConnection);
            return List.of();
        });

        Set<WatchingSessionRedisRepository.PresenceKey> missing = repository.refreshWatchers(List.of(
                new WatchingSessionRedisRepository.PresenceKey(contentId, existingWatcherId),
                new WatchingSessionRedisRepository.PresenceKey(contentId, missingWatcherId)
        ));

        assertThat(missing).containsExactly(
                new WatchingSessionRedisRepository.PresenceKey(contentId, missingWatcherId));
        verify(redisTemplate).executePipelined(any(RedisCallback.class));
        verify(stringCommands).set(
                any(),
                any(),
                eq(Expiration.milliseconds(30_000L)),
                eq(RedisStringCommands.SetOption.UPSERT));
    }

    @Test
    void refreshWatcher_returnsFalseWhenWatcherSummaryDoesNotExist() {
        var repository = repository();
        UUID contentId = UUID.randomUUID();
        UUID watcherId = UUID.randomUUID();
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.multiGet(key(contentId), List.of(watcherId.toString())))
                .thenReturn(Collections.singletonList(null));

        assertThat(repository.refreshWatcher(contentId, watcherId)).isFalse();
    }

    @Test
    void findActiveContentIds_ignoresInvalidIds() {
        var repository = repository();
        UUID contentId = UUID.randomUUID();
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("watching-sessions:active-contents"))
                .thenReturn(Set.of(contentId.toString(), "invalid-id"));

        assertThat(repository.findActiveContentIds()).containsExactly(contentId);
    }

    @Test
    void deleteByContentId_deletesKey() {
        var repository = repository();
        UUID contentId = UUID.randomUUID();
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        repository.deleteByContentId(contentId);
        verify(redisTemplate).delete(key(contentId));
        verify(setOperations).remove("watching-sessions:active-contents", contentId.toString());
    }

    @Test
    void nullId_throwsException() {
        assertThatThrownBy(() -> repository().countWatchers(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("id는 필수입니다.");
    }

    private WatchingSessionRedisRepositoryImpl repository() {
        return new WatchingSessionRedisRepositoryImpl(redisTemplate, new ObjectMapper());
    }

    private String key(UUID contentId) {
        return "watching-sessions:contents:%s:watcher-summaries".formatted(contentId);
    }

    private String heartbeatKey(UUID contentId, UUID watcherId) {
        return "watching-sessions:contents:%s:watchers:%s:heartbeat"
                .formatted(contentId, watcherId);
    }
}
