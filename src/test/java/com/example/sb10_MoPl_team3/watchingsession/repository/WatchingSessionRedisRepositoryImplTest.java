package com.example.sb10_MoPl_team3.watchingsession.repository;

import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Map;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WatchingSessionRedisRepositoryImplTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock HashOperations<String, Object, Object> hashOperations;
    @Mock ValueOperations<String, String> valueOperations;
    @Mock SetOperations<String, String> setOperations;

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
        when(redisTemplate.hasKey(heartbeatKey(contentId, watcher.userId()))).thenReturn(true);
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
