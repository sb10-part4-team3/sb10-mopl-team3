package com.example.sb10_MoPl_team3.global.sse;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface SseConnectionRepository {

    String saveEmitter(UUID userId, SseEmitter emitter);

    void deleteEmitter(UUID userId, String emitterId);

    Map<String, SseEmitter> findEmittersByUserId(UUID userId);

    void saveEvent(UUID userId, SseEventCache event);

    List<SseEventCache> findCachedEventsByUserId(UUID userId);

    List<SseEventCache> findCachedEventsAfter(UUID userId, String lastEventId);

    void deleteAllEmitters(UUID userId);

    void deleteAllCachedEvents(UUID userId);
}
