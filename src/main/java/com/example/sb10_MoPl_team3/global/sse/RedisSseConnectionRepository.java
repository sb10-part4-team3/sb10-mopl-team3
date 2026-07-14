package com.example.sb10_MoPl_team3.global.sse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisSseConnectionRepository implements SseConnectionRepository {

    private static final String EVENT_CACHE_KEY_PREFIX = "sse:users:";
    private static final String EVENT_CACHE_KEY_SUFFIX = ":events";

    private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, SseEmitter>> emitters =
            new ConcurrentHashMap<>();
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${sse.event-cache.max-size:100}")
    private int maxEventCacheSize;

    @Override
    public String saveEmitter(UUID userId, SseEmitter emitter) {
        String emitterId = UUID.randomUUID().toString();
        emitters.computeIfAbsent(userId, key -> new ConcurrentHashMap<>())
                .put(emitterId, emitter);
        return emitterId;
    }

    @Override
    public void deleteEmitter(UUID userId, String emitterId) {
        emitters.computeIfPresent(userId, (key, userEmitters) -> {
            userEmitters.remove(emitterId);
            return userEmitters.isEmpty() ? null : userEmitters;
        });
    }

    @Override
    public Map<String, SseEmitter> findEmittersByUserId(UUID userId) {
        Map<String, SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null) {
            return Collections.emptyMap();
        }
        return Map.copyOf(userEmitters);
    }

    @Override
    public void saveEvent(UUID userId, SseEventCache event) {
        SseEventCachePayload payload = SseEventCachePayload.from(event, objectMapper.valueToTree(event.data()));
        String key = eventCacheKey(userId);
        redisTemplate.opsForList().rightPush(key, serialize(payload));
        redisTemplate.opsForList().trim(key, -maxEventCacheSize, -1);
    }

    @Override
    public List<SseEventCache> findCachedEventsByUserId(UUID userId) {
        List<String> values = redisTemplate.opsForList().range(eventCacheKey(userId), 0, -1);
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(this::deserialize)
                .flatMap(List::stream)
                .map(SseEventCachePayload::toEventCache)
                .toList();
    }

    @Override
    public List<SseEventCache> findCachedEventsAfter(UUID userId, String lastEventId) {
        List<SseEventCache> cachedEvents = findCachedEventsByUserId(userId);
        if (lastEventId == null || lastEventId.isBlank()) {
            return cachedEvents;
        }

        List<SseEventCache> eventsAfterLastId = new ArrayList<>();
        boolean foundLastEvent = false;
        for (SseEventCache cachedEvent : cachedEvents) {
            if (foundLastEvent) {
                eventsAfterLastId.add(cachedEvent);
                continue;
            }
            foundLastEvent = cachedEvent.id().equals(lastEventId);
        }
        if (!foundLastEvent) {
            return cachedEvents;
        }
        return List.copyOf(eventsAfterLastId);
    }

    @Override
    public void deleteAllEmitters(UUID userId) {
        emitters.remove(userId);
    }

    @Override
    public void deleteAllCachedEvents(UUID userId) {
        redisTemplate.delete(eventCacheKey(userId));
    }

    @Override
    public void deleteCachedEvents(UUID userId, Predicate<SseEventCache> predicate) {
        removeCachedEvents(
                userId,
                payload -> predicate.test(payload.toEventCache()));
    }

    @Override
    public void deleteCachedEventByDataId(UUID userId, String eventName, UUID dataId) {
        removeCachedEvents(
                userId,
                payload -> matchesDataId(payload, eventName, dataId));
    }

    private List<SseEventCachePayload> findCachedEventPayloads(UUID userId) {
        List<String> values = redisTemplate.opsForList().range(eventCacheKey(userId), 0, -1);
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(this::deserialize)
                .flatMap(List::stream)
                .toList();
    }

    private void removeCachedEvents(UUID userId, Predicate<SseEventCachePayload> predicate) {
        String key = eventCacheKey(userId);
        findCachedEventPayloads(userId).stream()
                .filter(predicate)
                .map(this::serialize)
                .forEach(value -> redisTemplate.opsForList().remove(key, 1, value));
    }

    private boolean matchesDataId(SseEventCachePayload payload, String eventName, UUID dataId) {
        JsonNode idNode = payload.data().get("id");
        return eventName.equals(payload.name())
                && idNode != null
                && dataId.toString().equals(idNode.asText());
    }

    private String serialize(SseEventCachePayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("SSE 이벤트 캐시를 Redis 값으로 변환할 수 없습니다.", exception);
        }
    }

    private List<SseEventCachePayload> deserialize(String value) {
        try {
            return List.of(objectMapper.readValue(value, SseEventCachePayload.class));
        } catch (JsonProcessingException exception) {
            log.warn(
                    "Redis에 저장된 SSE 이벤트 캐시를 역직렬화할 수 없습니다. payloadLength={}, payloadFingerprint={}",
                    value.length(),
                    Integer.toHexString(value.hashCode()),
                    exception);
            return List.of();
        }
    }

    private String eventCacheKey(UUID userId) {
        return EVENT_CACHE_KEY_PREFIX + userId + EVENT_CACHE_KEY_SUFFIX;
    }
}
