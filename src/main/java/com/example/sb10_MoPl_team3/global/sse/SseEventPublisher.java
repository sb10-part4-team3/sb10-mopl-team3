package com.example.sb10_MoPl_team3.global.sse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class SseEventPublisher {

    public static final String NOTIFICATIONS_EVENT = "notifications";
    public static final String DIRECT_MESSAGES_EVENT = "direct-messages";
    public static final String BROADCAST_CHANNEL = "sse:broadcast";

    private final SseConnectionRepository connectionRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

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
        redisTemplate.convertAndSend(
                BROADCAST_CHANNEL,
                serialize(new SseBroadcastMessage(
                        userId,
                        SseEventCachePayload.from(event, objectMapper.valueToTree(data)))));
        return eventId;
    }

    private String serialize(SseBroadcastMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("SSE broadcast 메시지를 Redis 값으로 변환할 수 없습니다.", exception);
        }
    }
}
