package com.example.sb10_MoPl_team3.global.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class SseEventPublisherTest {

    @Mock SseConnectionRepository connectionRepository;
    @Mock SseEmitter emitter;
    @InjectMocks SseEventPublisher publisher;

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void publishAfterCommit_publishesImmediatelyWithoutTransactionSynchronization() {
        UUID userId = UUID.randomUUID();
        given(connectionRepository.findEmittersByUserId(userId)).willReturn(Map.of());

        publisher.publishAfterCommit(
                userId, SseEventPublisher.NOTIFICATIONS_EVENT, "data");

        then(connectionRepository).should().saveEvent(
                org.mockito.ArgumentMatchers.eq(userId), any(SseEventCache.class));
    }

    @Test
    void publishAfterCommit_defersPublishingUntilTransactionCommit() {
        UUID userId = UUID.randomUUID();
        given(connectionRepository.findEmittersByUserId(userId)).willReturn(Map.of());
        TransactionSynchronizationManager.initSynchronization();

        publisher.publishAfterCommit(
                userId, SseEventPublisher.NOTIFICATIONS_EVENT, "data");

        then(connectionRepository).should(never()).saveEvent(
                org.mockito.ArgumentMatchers.eq(userId), any(SseEventCache.class));

        TransactionSynchronizationUtils.triggerAfterCommit();

        then(connectionRepository).should().saveEvent(
                org.mockito.ArgumentMatchers.eq(userId), any(SseEventCache.class));
    }

    @Test
    void publish_cachesAndSendsEventToAllActiveConnections() throws Exception {
        UUID userId = UUID.randomUUID();
        given(connectionRepository.findEmittersByUserId(userId))
                .willReturn(Map.of("emitter-id", emitter));

        String eventId = publisher.publish(
                userId, SseEventPublisher.NOTIFICATIONS_EVENT, "data");

        ArgumentCaptor<SseEventCache> cacheCaptor = ArgumentCaptor.forClass(SseEventCache.class);
        then(connectionRepository).should().saveEvent(
                org.mockito.ArgumentMatchers.eq(userId), cacheCaptor.capture());
        assertThat(cacheCaptor.getValue().id()).isEqualTo(eventId);
        assertThat(cacheCaptor.getValue().name())
                .isEqualTo(SseEventPublisher.NOTIFICATIONS_EVENT);
        assertThat(cacheCaptor.getValue().data()).isEqualTo("data");
        then(emitter).should().send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void publish_removesEmitterWhenSendingFails() throws Exception {
        UUID userId = UUID.randomUUID();
        given(connectionRepository.findEmittersByUserId(userId))
                .willReturn(Map.of("failed-emitter", emitter));
        willThrow(new IOException("connection closed"))
                .given(emitter).send(any(SseEmitter.SseEventBuilder.class));

        publisher.publish(userId, SseEventPublisher.DIRECT_MESSAGES_EVENT, "data");

        then(connectionRepository).should().deleteEmitter(userId, "failed-emitter");
    }
}
