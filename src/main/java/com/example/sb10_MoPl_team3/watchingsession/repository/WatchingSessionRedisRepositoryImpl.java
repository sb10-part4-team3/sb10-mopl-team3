package com.example.sb10_MoPl_team3.watchingsession.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class WatchingSessionRedisRepositoryImpl implements WatchingSessionRedisRepository {

    private static final String KEY_PREFIX = "watching-sessions:contents:";
    private static final String KEY_SUFFIX = ":watchers";

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean addWatcher(UUID contentId, UUID watcherId) {
        Long addedCount = redisTemplate.opsForSet()
                .add(key(contentId), value(watcherId));

        return addedCount != null && addedCount > 0;
    }

    @Override
    public boolean removeWatcher(UUID contentId, UUID watcherId) {
        Long removedCount = redisTemplate.opsForSet()
                .remove(key(contentId), value(watcherId));

        return removedCount != null && removedCount > 0;
    }

    @Override
    public Set<UUID> findWatcherIds(UUID contentId) {
        Set<String> watcherIds = redisTemplate.opsForSet().members(key(contentId));
        if (watcherIds == null || watcherIds.isEmpty()) {
            return Collections.emptySet();
        }

        return watcherIds.stream()
                .map(this::parseUuid)
                .flatMap(Optional::stream)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public long countWatchers(UUID contentId) {
        Long count = redisTemplate.opsForSet().size(key(contentId));
        return count != null ? count : 0L;
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

    private Optional<UUID> parseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
