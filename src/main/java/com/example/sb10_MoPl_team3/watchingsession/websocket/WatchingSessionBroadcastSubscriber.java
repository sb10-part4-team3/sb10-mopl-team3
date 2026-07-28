package com.example.sb10_MoPl_team3.watchingsession.websocket;

import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionChange;
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
public class WatchingSessionBroadcastSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final WatchingSessionLocalEventDispatcher localEventDispatcher;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            WatchingSessionChange change = objectMapper.readValue(payload, WatchingSessionChange.class);
            localEventDispatcher.dispatch(change, "redis-broadcast");
        } catch (JsonProcessingException exception) {
            log.warn(
                    "시청 세션 broadcast 메시지를 역직렬화할 수 없습니다. payloadLength={}, payloadFingerprint={}",
                    payload.length(),
                    Integer.toHexString(payload.hashCode()),
                    exception);
        }
    }
}
