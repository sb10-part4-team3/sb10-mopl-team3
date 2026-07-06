package com.example.sb10_MoPl_team3.global.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class SseEventPublisherTest {

    @Mock SseConnectionRepository connectionRepository;
    @Mock SseEmitter emitter;
    @InjectMocks SseEventPublisher publisher;

    @Test
    void publish_cachesAndSendsEventToAllActiveConnections() throws Exception {
        UUID userId = UUID.randomUUID();
        given(connectionRepository.findEmittersByUserId(userId))
                .willReturn(Map.of("emitter-id", emitter));

        String eventId = publisher.publish(userId, "notifications", "data");

        ArgumentCaptor<SseEventCache> cacheCaptor = ArgumentCaptor.forClass(SseEventCache.class);
        then(connectionRepository).should().saveEvent(
                org.mockito.ArgumentMatchers.eq(userId), cacheCaptor.capture());
        assertThat(cacheCaptor.getValue().id()).isEqualTo(eventId);
        assertThat(cacheCaptor.getValue().name()).isEqualTo("notifications");
        assertThat(cacheCaptor.getValue().data()).isEqualTo("data");
        then(emitter).should().send(any(SseEmitter.SseEventBuilder.class));
    }
}
