package com.example.sb10_MoPl_team3.global.sse;

import java.util.Objects;
import java.util.UUID;

public record SseBroadcastMessage(
        UUID userId,
        SseEventCachePayload event
) {

    public SseBroadcastMessage {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(event, "event must not be null");
    }
}
