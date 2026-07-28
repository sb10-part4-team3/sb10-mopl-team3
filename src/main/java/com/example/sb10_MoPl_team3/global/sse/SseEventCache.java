package com.example.sb10_MoPl_team3.global.sse;

import java.time.Instant;
import java.util.Objects;

public record SseEventCache(
        String id,
        String name,
        Object data,
        Instant createdAt
) {

    public SseEventCache {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(data, "data must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static SseEventCache of(String id, String name, Object data) {
        return new SseEventCache(id, name, data, Instant.now());
    }
}
