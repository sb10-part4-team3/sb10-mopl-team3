package com.example.sb10_MoPl_team3.global.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

@ExtendWith(MockitoExtension.class)
class SseEventPublisherTest {

    @Mock SseConnectionRepository connectionRepository;
    @Mock StringRedisTemplate redisTemplate;
    SseEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new SseEventPublisher(
                connectionRepository,
                redisTemplate,
                new ObjectMapper().findAndRegisterModules());
    }

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void publishAfterCommit_publishesImmediatelyWithoutTransactionSynchronization() {
        UUID userId = UUID.randomUUID();

        publisher.publishAfterCommit(
                userId, SseEventPublisher.NOTIFICATIONS_EVENT, "data");

        then(connectionRepository).should().saveEvent(
                org.mockito.ArgumentMatchers.eq(userId), any(SseEventCache.class));
        then(redisTemplate).should().convertAndSend(
                org.mockito.ArgumentMatchers.eq(SseEventPublisher.BROADCAST_CHANNEL),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void publishAfterCommit_defersPublishingUntilTransactionCommit() {
        UUID userId = UUID.randomUUID();
        TransactionSynchronizationManager.initSynchronization();

        publisher.publishAfterCommit(
                userId, SseEventPublisher.NOTIFICATIONS_EVENT, "data");

        then(connectionRepository).should(never()).saveEvent(
                org.mockito.ArgumentMatchers.eq(userId), any(SseEventCache.class));
        then(redisTemplate).should(never()).convertAndSend(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());

        TransactionSynchronizationUtils.triggerAfterCommit();

        then(connectionRepository).should().saveEvent(
                org.mockito.ArgumentMatchers.eq(userId), any(SseEventCache.class));
        then(redisTemplate).should().convertAndSend(
                org.mockito.ArgumentMatchers.eq(SseEventPublisher.BROADCAST_CHANNEL),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void publish_cachesAndBroadcastsEvent() {
        UUID userId = UUID.randomUUID();

        String eventId = publisher.publish(
                userId, SseEventPublisher.NOTIFICATIONS_EVENT, "data");

        ArgumentCaptor<SseEventCache> cacheCaptor = ArgumentCaptor.forClass(SseEventCache.class);
        then(connectionRepository).should().saveEvent(
                org.mockito.ArgumentMatchers.eq(userId), cacheCaptor.capture());
        assertThat(cacheCaptor.getValue().id()).isEqualTo(eventId);
        assertThat(cacheCaptor.getValue().name())
                .isEqualTo(SseEventPublisher.NOTIFICATIONS_EVENT);
        assertThat(cacheCaptor.getValue().data()).isEqualTo("data");
        then(redisTemplate).should().convertAndSend(
                org.mockito.ArgumentMatchers.eq(SseEventPublisher.BROADCAST_CHANNEL),
                org.mockito.ArgumentMatchers.contains(eventId));
    }
}
