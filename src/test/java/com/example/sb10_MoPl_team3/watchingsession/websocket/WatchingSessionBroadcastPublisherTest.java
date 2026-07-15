package com.example.sb10_MoPl_team3.watchingsession.websocket;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.dto.ContentSummary;
import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;
import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionChange;
import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionChangeType;
import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class WatchingSessionBroadcastPublisherTest {

    @Mock StringRedisTemplate redisTemplate;

    @Test
    void publish_sendsSerializedChangeToRedisChannel() {
        WatchingSessionBroadcastPublisher publisher = new WatchingSessionBroadcastPublisher(
                redisTemplate,
                new ObjectMapper().findAndRegisterModules());
        WatchingSessionChange change = change(UUID.randomUUID(), UUID.randomUUID());

        publisher.publish(change);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        then(redisTemplate).should().convertAndSend(
                org.mockito.ArgumentMatchers.eq(WatchingSessionBroadcastPublisher.BROADCAST_CHANNEL),
                payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).contains(change.type().name());
        assertThat(payloadCaptor.getValue()).contains(change.watchingSession().watcher().userId().toString());
    }

    @Test
    void publish_throwsIllegalStateExceptionWhenSerializationFails() throws Exception {
        ObjectMapper objectMapper = org.mockito.Mockito.mock(ObjectMapper.class);
        WatchingSessionBroadcastPublisher publisher = new WatchingSessionBroadcastPublisher(
                redisTemplate,
                objectMapper);
        WatchingSessionChange change = change(UUID.randomUUID(), UUID.randomUUID());
        given(objectMapper.writeValueAsString(any()))
                .willThrow(new JsonProcessingException("boom") {
                });

        assertThatThrownBy(() -> publisher.publish(change))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("시청 세션 변경 메시지를 Redis 값으로 변환할 수 없습니다.");
    }

    private WatchingSessionChange change(UUID contentId, UUID watcherId) {
        return new WatchingSessionChange(
                WatchingSessionChangeType.JOIN,
                new WatchingSessionDto(
                        UUID.randomUUID(),
                        Instant.now(),
                        new UserSummary(watcherId, "시청자", null),
                        new ContentSummary(
                                contentId,
                                ContentType.MOVIE,
                                "콘텐츠",
                                "설명",
                                "thumbnail",
                                List.of(),
                                0.0,
                                0
                        )
                ),
                1
        );
    }
}
