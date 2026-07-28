package com.example.sb10_MoPl_team3.watchingsession.websocket;

import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionChange;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WatchingSessionBroadcastPublisher {

    public static final String BROADCAST_CHANNEL = "watching-session:broadcast";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(WatchingSessionChange change) {
        redisTemplate.convertAndSend(BROADCAST_CHANNEL, serialize(change));
    }

    private String serialize(WatchingSessionChange change) {
        try {
            return objectMapper.writeValueAsString(change);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("시청 세션 변경 메시지를 Redis 값으로 변환할 수 없습니다.", exception);
        }
    }
}
