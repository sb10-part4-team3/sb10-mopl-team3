package com.example.sb10_MoPl_team3.global.sse;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseEventPublisher {

    public static final String NOTIFICATIONS_EVENT = "notifications";
    public static final String DIRECT_MESSAGES_EVENT = "direct-messages";

    private final SseConnectionRepository connectionRepository;

    public void publishAfterCommit(UUID userId, String eventName, Object data) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publish(userId, eventName, data);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publish(userId, eventName, data);
            }
        });
    }

    public String publish(UUID userId, String eventName, Object data) {
        String eventId = UUID.randomUUID().toString();
        SseEventCache event = SseEventCache.of(eventId, eventName, data);
        connectionRepository.saveEvent(userId, event);

        for (Map.Entry<String, SseEmitter> entry
                : connectionRepository.findEmittersByUserId(userId).entrySet()) {
            send(userId, entry.getKey(), entry.getValue(), event);
        }
        return eventId;
    }

    private void send(
            UUID userId,
            String emitterId,
            SseEmitter emitter,
            SseEventCache event
    ) {
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
