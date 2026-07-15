package com.example.sb10_MoPl_team3.watchingsession.repository;

import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
        return refreshWatchers(List.of(new PresenceKey(contentId, watcherId))).isEmpty();
    }

    @Override
    public Set<PresenceKey> refreshWatchers(Collection<PresenceKey> presences) {
        if (presences == null || presences.isEmpty()) {
            return Set.of();
        }

        Map<UUID, List<PresenceKey>> presencesByContent = presences.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(PresenceKey::contentId));
        Set<PresenceKey> missing = new HashSet<>();
        List<PresenceKey> existing = new ArrayList<>();

        presencesByContent.forEach((contentId, contentPresences) -> {
            List<Object> fields = contentPresences.stream()
                    .map(PresenceKey::watcherId)
                    .map(this::value)
                    .map(Object.class::cast)
                    .toList();
            List<Object> watcherSummaries = redisTemplate.opsForHash().multiGet(key(contentId), fields);
            for (int i = 0; i < contentPresences.size(); i++) {
                PresenceKey presence = contentPresences.get(i);
                Object watcherSummary = watcherSummaries != null && i < watcherSummaries.size()
                        ? watcherSummaries.get(i)
                        : null;
                if (watcherSummary == null) {
                    missing.add(presence);
                    continue;
                }
                existing.add(presence);
            }
        });

        refreshHeartbeats(existing);
        return Set.copyOf(missing);
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
        List<Object> validFields = new ArrayList<>();
        List<String> heartbeatKeys = new ArrayList<>();

        entries.forEach((field, payload) -> {
            UUID watcherId = parseUuid(field.toString()).orElse(null);
            if (watcherId == null) {
                fieldsToRemove.add(field);
                return;
            }
            validFields.add(field);
            heartbeatKeys.add(heartbeatKey(contentId, watcherId));
        });

        if (!heartbeatKeys.isEmpty()) {
            List<String> heartbeats = redisTemplate.opsForValue().multiGet(heartbeatKeys);
            for (int i = 0; i < validFields.size(); i++) {
                boolean alive = heartbeats != null
                        && i < heartbeats.size()
                        && heartbeats.get(i) != null;
                if (alive) {
                    continue;
                }
                Object field = validFields.get(i);
                fieldsToRemove.add(field);
                deserialize(entries.get(field).toString()).ifPresent(removed::add);
            }
        }

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
        return count != null ? count : 0L;
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
    public void forEachActiveContentId(Consumer<UUID> consumer) {
        Objects.requireNonNull(consumer, "consumer는 필수입니다.");
        try (Cursor<String> cursor = redisTemplate.opsForSet()
                .scan(ACTIVE_CONTENTS_KEY, ScanOptions.scanOptions().count(100).build())) {
            while (cursor.hasNext()) {
                parseUuid(cursor.next()).ifPresent(consumer);
            }
        }
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

    private void refreshHeartbeats(List<PresenceKey> presences) {
        if (presences.isEmpty()) {
            return;
        }
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            long expirationSeconds = Math.max(1L, presenceTtl.toSeconds());
            for (PresenceKey presence : presences) {
                byte[] heartbeatKey = serialize(heartbeatKey(presence.contentId(), presence.watcherId()));
                byte[] heartbeatValue = serialize(HEARTBEAT_VALUE);
                connection.stringCommands().set(
                        heartbeatKey,
                        heartbeatValue,
                        Expiration.seconds(expirationSeconds),
                        RedisStringCommands.SetOption.UPSERT);
                connection.setCommands().sAdd(
                        serialize(ACTIVE_CONTENTS_KEY),
                        serialize(value(presence.contentId())));
            }
            return null;
        });
    }

    private byte[] serialize(String value) {
        return redisTemplate.getStringSerializer().serialize(value);
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
