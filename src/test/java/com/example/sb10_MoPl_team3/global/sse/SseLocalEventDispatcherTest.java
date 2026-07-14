package com.example.sb10_MoPl_team3.global.sse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class SseLocalEventDispatcherTest {

    @Mock SseConnectionRepository connectionRepository;
    @Mock SseEmitter emitter;

    @Test
    void dispatch_sendsEventToLocalConnections() throws Exception {
        UUID userId = UUID.randomUUID();
        given(connectionRepository.findEmittersByUserId(userId))
                .willReturn(Map.of("emitter-id", emitter));
        SseLocalEventDispatcher dispatcher = new SseLocalEventDispatcher(connectionRepository);

        dispatcher.dispatch(userId, SseEventCache.of("event-1", "notifications", "data"));

        then(emitter).should().send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void dispatch_removesEmitterWhenSendingFails() throws Exception {
        UUID userId = UUID.randomUUID();
        given(connectionRepository.findEmittersByUserId(userId))
                .willReturn(Map.of("failed-emitter", emitter));
        willThrow(new IOException("connection closed"))
                .given(emitter).send(any(SseEmitter.SseEventBuilder.class));
        SseLocalEventDispatcher dispatcher = new SseLocalEventDispatcher(connectionRepository);

        dispatcher.dispatch(userId, SseEventCache.of("event-1", "notifications", "data"));

        then(connectionRepository).should().deleteEmitter(userId, "failed-emitter");
    }
}
