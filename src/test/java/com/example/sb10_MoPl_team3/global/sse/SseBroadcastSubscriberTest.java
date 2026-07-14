package com.example.sb10_MoPl_team3.global.sse;

import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;

@ExtendWith(MockitoExtension.class)
class SseBroadcastSubscriberTest {

    @Mock SseLocalEventDispatcher localEventDispatcher;
    @Mock Message redisMessage;

    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void onMessage_dispatchesBroadcastEventToLocalEmitters() throws Exception {
        UUID userId = UUID.randomUUID();
        SseEventCache event = SseEventCache.of("event-1", "notifications", "data");
        SseBroadcastMessage message = new SseBroadcastMessage(
                userId,
                SseEventCachePayload.from(event, objectMapper.valueToTree(event.data())));
        SseBroadcastSubscriber subscriber =
                new SseBroadcastSubscriber(objectMapper, localEventDispatcher);
        given(redisMessage.getBody()).willReturn(objectMapper.writeValueAsBytes(message));

        subscriber.onMessage(redisMessage, null);

        ArgumentCaptor<SseEventCache> eventCaptor = ArgumentCaptor.forClass(SseEventCache.class);
        then(localEventDispatcher).should().dispatch(
                org.mockito.ArgumentMatchers.eq(userId),
                eventCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(eventCaptor.getValue().id()).isEqualTo("event-1");
        org.assertj.core.api.Assertions.assertThat(eventCaptor.getValue().name()).isEqualTo("notifications");
    }
}
