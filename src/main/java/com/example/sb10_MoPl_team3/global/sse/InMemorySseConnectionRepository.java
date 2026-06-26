package com.example.sb10_MoPl_team3.global.sse;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Repository
public class InMemorySseConnectionRepository implements SseConnectionRepository {

    private static final int MAX_EVENT_CACHE_SIZE = 100;

    private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ConcurrentLinkedDeque<SseEventCache>> eventCaches = new ConcurrentHashMap<>();

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
        ConcurrentLinkedDeque<SseEventCache> userEventCache =
                eventCaches.computeIfAbsent(userId, key -> new ConcurrentLinkedDeque<>());

        userEventCache.addLast(event);
        trimEventCache(userEventCache);
    }

    @Override
    public List<SseEventCache> findCachedEventsByUserId(UUID userId) {
        ConcurrentLinkedDeque<SseEventCache> userEventCache = eventCaches.get(userId);
        if (userEventCache == null) {
            return List.of();
        }

        return List.copyOf(userEventCache);
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
        eventCaches.remove(userId);
    }

    private void trimEventCache(ConcurrentLinkedDeque<SseEventCache> eventCache) {
        while (eventCache.size() > MAX_EVENT_CACHE_SIZE) {
            eventCache.pollFirst();
        }
    }
}
