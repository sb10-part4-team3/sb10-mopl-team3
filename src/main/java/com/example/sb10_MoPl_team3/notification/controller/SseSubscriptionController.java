package com.example.sb10_MoPl_team3.notification.controller;

import com.example.sb10_MoPl_team3.global.security.AuthUser;
import com.example.sb10_MoPl_team3.global.sse.SseConnectionRepository;
import com.example.sb10_MoPl_team3.global.sse.SseEventCache;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class SseSubscriptionController {

    private static final long SSE_TIMEOUT_MILLIS = 30 * 60 * 1000L;
    private static final String LAST_EVENT_ID_HEADER = "Last-Event-ID";

    private final SseConnectionRepository sseConnectionRepository;

    @GetMapping(value = "/api/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestHeader(value = LAST_EVENT_ID_HEADER, required = false) String lastEventId
    ) {
        UUID userId = authUser.userId();
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        String emitterId = sseConnectionRepository.saveEmitter(userId, emitter);

        emitter.onCompletion(() -> sseConnectionRepository.deleteEmitter(userId, emitterId));
        emitter.onTimeout(() -> sseConnectionRepository.deleteEmitter(userId, emitterId));
        emitter.onError(error -> sseConnectionRepository.deleteEmitter(userId, emitterId));

        replayCachedEvents(userId, lastEventId, emitter);

        return emitter;
    }

    private void replayCachedEvents(UUID userId, String lastEventId, SseEmitter emitter) {
        for (SseEventCache event : sseConnectionRepository.findCachedEventsAfter(userId, lastEventId)) {
            try {
                emitter.send(SseEmitter.event()
                        .id(event.id())
                        .name(event.name())
                        .data(event.data()));
            } catch (IOException | IllegalStateException e) {
                emitter.completeWithError(e);
                return;
            }
        }
    }
}
