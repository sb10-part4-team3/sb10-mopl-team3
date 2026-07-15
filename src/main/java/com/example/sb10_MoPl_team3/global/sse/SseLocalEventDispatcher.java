package com.example.sb10_MoPl_team3.global.sse;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
@RequiredArgsConstructor
public class SseLocalEventDispatcher {

    private final SseConnectionRepository connectionRepository;

    public void dispatch(UUID userId, SseEventCache event) {
        for (Map.Entry<String, SseEmitter> entry
                : connectionRepository.findEmittersByUserId(userId).entrySet()) {
            send(userId, entry.getKey(), entry.getValue(), event);
        }
    }

    private void send(UUID userId, String emitterId, SseEmitter emitter, SseEventCache event) {
        try {
            emitter.send(SseEmitter.event()
                    .id(event.id())
                    .name(event.name())
                    .data(event.data()));
        } catch (IOException | IllegalStateException exception) {
            connectionRepository.deleteEmitter(userId, emitterId);
            log.debug("SSE 전송 실패로 연결을 제거합니다: userId={}, emitterId={}",
                    userId, emitterId, exception);
        }
    }
}
