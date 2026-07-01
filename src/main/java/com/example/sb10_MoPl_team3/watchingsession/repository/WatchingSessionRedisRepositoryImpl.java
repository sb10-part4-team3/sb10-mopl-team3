package com.example.sb10_MoPl_team3.watchingsession.repository;

import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class WatchingSessionRedisRepositoryImpl implements WatchingSessionRedisRepository {

    private static final String KEY_PREFIX = "watching-sessions:contents:";
    private static final String KEY_SUFFIX = ":watcher-summaries";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public boolean addWatcher(UUID contentId, UserSummary watcher) {
        Objects.requireNonNull(watcher, "watcher는 필수입니다.");
        String field = value(watcher.userId());
        String json = serialize(watcher);
        Boolean added = redisTemplate.opsForHash().putIfAbsent(key(contentId), field, json);
        if (!added) {
            redisTemplate.opsForHash().put(key(contentId), field, json);
        }
        return added;
    }

    @Override
    public boolean removeWatcher(UUID contentId, UUID watcherId) {
        Long removedCount = redisTemplate.opsForHash()
                .delete(key(contentId), value(watcherId));

        return removedCount > 0;
    }

    @Override
    public List<UserSummary> findWatchers(UUID contentId) {
        return redisTemplate.opsForHash().values(key(contentId)).stream()
                .map(Object::toString)
                .map(this::deserialize)
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public long countWatchers(UUID contentId) {
        Long count = redisTemplate.opsForHash().size(key(contentId));
        return count;
    }

    @Override
    public void deleteByContentId(UUID contentId) {
        redisTemplate.delete(key(contentId));
    }

    private String key(UUID contentId) {
        return KEY_PREFIX + value(contentId) + KEY_SUFFIX;
    }

    private String value(UUID id) {
        return Objects.requireNonNull(id, "id는 필수입니다.").toString();
    }

    private String serialize(UserSummary watcher) {
        try {
            return objectMapper.writeValueAsString(watcher);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("시청자 정보를 Redis 값으로 변환할 수 없습니다.", exception);
        }
    }

    private Optional<UserSummary> deserialize(String value) {
        try {
            return Optional.of(objectMapper.readValue(value, UserSummary.class));
        } catch (JsonProcessingException exception) {
            log.warn(
                    "Redis에 저장된 시청자 정보를 역직렬화할 수 없습니다. payloadLength="
                             value.length()
                             ", payloadFingerprint="
                             Integer.toHexString(value.hashCode()),
                    exception);
            return Optional.empty();
        }
    }
}
