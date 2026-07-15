package com.example.sb10_MoPl_team3.global.sse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SseBroadcastSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SseLocalEventDispatcher localEventDispatcher;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            SseBroadcastMessage broadcastMessage =
                    objectMapper.readValue(payload, SseBroadcastMessage.class);
            localEventDispatcher.dispatch(
                    broadcastMessage.userId(),
                    broadcastMessage.event().toEventCache());
        } catch (JsonProcessingException exception) {
            log.warn(
                    "SSE broadcast 메시지를 역직렬화할 수 없습니다. payloadLength={}, payloadFingerprint={}",
                    payload.length(),
                    Integer.toHexString(payload.hashCode()),
                    exception);
        }
    }
}
