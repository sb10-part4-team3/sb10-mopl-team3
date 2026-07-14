package com.example.sb10_MoPl_team3.global.sse;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.Objects;

public record SseEventCachePayload(
        String id,
        String name,
        JsonNode data,
        Instant createdAt
) {

    public SseEventCachePayload {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(data, "data must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static SseEventCachePayload from(SseEventCache event, JsonNode data) {
        return new SseEventCachePayload(event.id(), event.name(), data, event.createdAt());
    }

    public SseEventCache toEventCache() {
        return new SseEventCache(id, name, data, createdAt);
    }
}
