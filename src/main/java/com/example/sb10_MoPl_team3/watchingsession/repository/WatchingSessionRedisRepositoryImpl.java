package com.example.sb10_MoPl_team3.watchingsession.repository;

import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class WatchingSessionRedisRepositoryImpl implements WatchingSessionRedisRepository {

    private static final String KEY_PREFIX = "watching-sessions:contents:";
    private static final String KEY_SUFFIX = ":watcher-summaries";
    private static final String ACTIVE_CONTENTS_KEY = "watching-sessions:active-contents";
    private static final String HEARTBEAT_KEY_FORMAT = "watching-sessions:contents:%s:watchers:%s:heartbeat";
    private static final String HEARTBEAT_VALUE = "1";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${watching-session.presence.ttl:30s}")
    private Duration presenceTtl = Duration.ofSeconds(30);

    @Override
    public boolean addWatcher(UUID contentId, UserSummary watcher) {
        Objects.requireNonNull(watcher, "watcher는 필수입니다.");
        String field = value(watcher.userId());
        String json = serialize(watcher);
        Boolean added = redisTemplate.opsForHash().putIfAbsent(key(contentId), field, json);
        if (!added) {
            redisTemplate.opsForHash().put(key(contentId), field, json);
        }
        redisTemplate.opsForValue().set(heartbeatKey(contentId, watcher.userId()), HEARTBEAT_VALUE, presenceTtl);
        redisTemplate.opsForSet().add(ACTIVE_CONTENTS_KEY, value(contentId));
        return added;
    }

    @Override
    public boolean refreshWatcher(UUID contentId, UUID watcherId) {
        String contentKey = key(contentId);
        String field = value(watcherId);
        Boolean exists = redisTemplate.opsForHash().hasKey(contentKey, field);
        if (!Boolean.TRUE.equals(exists)) {
            return false;
        }
        redisTemplate.opsForValue().set(heartbeatKey(contentId, watcherId), HEARTBEAT_VALUE, presenceTtl);
        redisTemplate.opsForSet().add(ACTIVE_CONTENTS_KEY, value(contentId));
        return true;
    }

    @Override
    public boolean removeWatcher(UUID contentId, UUID watcherId) {
        Long removedCount = redisTemplate.opsForHash()
                .delete(key(contentId), value(watcherId));
        redisTemplate.delete(heartbeatKey(contentId, watcherId));
        removeContentFromActiveSetIfEmpty(contentId);

        return removedCount != null && removedCount > 0;
    }

    @Override
    public List<UserSummary> removeStaleWatchers(UUID contentId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key(contentId));
        if (entries == null || entries.isEmpty()) {
            removeContentFromActiveSetIfEmpty(contentId);
            return List.of();
        }
        List<UserSummary> removed = new ArrayList<>();
        List<Object> fieldsToRemove = new ArrayList<>();
        entries.forEach((field, payload) -> {
            UUID watcherId = parseUuid(field.toString()).orElse(null);
            if (watcherId == null) {
                fieldsToRemove.add(field);
                return;
            }
            Boolean alive = redisTemplate.hasKey(heartbeatKey(contentId, watcherId));
            if (!Boolean.TRUE.equals(alive)) {
                fieldsToRemove.add(field);
                deserialize(payload.toString()).ifPresent(removed::add);
            }
        });

        if (!fieldsToRemove.isEmpty()) {
            redisTemplate.opsForHash().delete(key(contentId), fieldsToRemove.toArray());
        }
        removeContentFromActiveSetIfEmpty(contentId);
        return List.copyOf(removed);
    }

    @Override
    public List<UserSummary> findWatchers(UUID contentId) {
        removeStaleWatchers(contentId);
        return redisTemplate.opsForHash().values(key(contentId)).stream()
                .map(Object::toString)
                .map(this::deserialize)
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public long countWatchers(UUID contentId) {
        removeStaleWatchers(contentId);
        Long count = redisTemplate.opsForHash().size(key(contentId));
        return count;
    }

    @Override
    public Set<UUID> findActiveContentIds() {
        Set<String> values = redisTemplate.opsForSet().members(ACTIVE_CONTENTS_KEY);
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        Set<UUID> ids = new HashSet<>();
        for (String value : values) {
            parseUuid(value).ifPresent(ids::add);
        }
        return Set.copyOf(ids);
    }

    @Override
    public void deleteByContentId(UUID contentId) {
        redisTemplate.delete(key(contentId));
        redisTemplate.opsForSet().remove(ACTIVE_CONTENTS_KEY, value(contentId));
    }

    private String key(UUID contentId) {
        return KEY_PREFIX + value(contentId) + KEY_SUFFIX;
    }

    private String heartbeatKey(UUID contentId, UUID watcherId) {
        return HEARTBEAT_KEY_FORMAT.formatted(value(contentId), value(watcherId));
    }

    private void removeContentFromActiveSetIfEmpty(UUID contentId) {
        Long count = redisTemplate.opsForHash().size(key(contentId));
        if (count != null && count == 0) {
            redisTemplate.opsForSet().remove(ACTIVE_CONTENTS_KEY, value(contentId));
        }
    }

    private String value(UUID id) {
        return Objects.requireNonNull(id, "id는 필수입니다.").toString();
    }

    private Optional<UUID> parseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            log.warn("Redis에 저장된 UUID 값을 파싱할 수 없습니다. value={}", value, exception);
            return Optional.empty();
        }
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
                "Redis에 저장된 시청자 정보를 역직렬화할 수 없습니다. payloadLength={}, payloadFingerprint={}",
                value.length(),
                Integer.toHexString(value.hashCode()),
                exception
            );
            return Optional.empty();
        }
    }
}
