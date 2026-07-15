package com.example.sb10_MoPl_team3.notification.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.sb10_MoPl_team3.notification.config.NotificationKafkaTopics;
import com.example.sb10_MoPl_team3.notification.entity.NotificationFanoutOutbox;
import com.example.sb10_MoPl_team3.notification.enums.NotificationLevel;
import com.example.sb10_MoPl_team3.notification.event.NotificationAudienceType;
import com.example.sb10_MoPl_team3.notification.event.NotificationFanoutEvent;
import com.example.sb10_MoPl_team3.notification.kafka.NotificationFanoutKafkaMessage;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class NotificationFanoutOutboxRelayTest {

    @Mock
    NotificationFanoutOutboxClaimService claimService;

    @Mock
    NotificationFanoutOutboxStatusService statusService;

    @Mock
    KafkaTemplate<String, NotificationFanoutKafkaMessage> kafkaTemplate;

    Clock clock = Clock.fixed(Instant.parse("2026-07-09T00:00:00Z"), ZoneOffset.UTC);

    NotificationFanoutOutboxRelay relay;

    @BeforeEach
    void setUp() {
        relay = new NotificationFanoutOutboxRelay(
                claimService,
                statusService,
                kafkaTemplate,
                clock
        );
    }

    @Test
    @DisplayName("claim한 outbox를 Kafka로 발행하고 성공 상태를 기록한다")
    void publishPending_sendsKafkaMessageAndMarksPublished() {
        UUID outboxId = UUID.randomUUID();
        NotificationFanoutOutbox outbox = outbox(outboxId);
        given(claimService.claimBatch()).willReturn(List.of(outbox));
        given(kafkaTemplate.send(
                eq(NotificationKafkaTopics.FANOUT),
                eq(outboxId.toString()),
                any(NotificationFanoutKafkaMessage.class)
        )).willReturn(CompletableFuture.completedFuture(null));

        relay.publishPending();

        then(kafkaTemplate).should().send(
                eq(NotificationKafkaTopics.FANOUT),
                eq(outboxId.toString()),
                any(NotificationFanoutKafkaMessage.class)
        );
        then(statusService).should().markPublished(outboxId, clock.instant());
    }

    @Test
    @DisplayName("Kafka 발행 실패 시 실패 상태를 기록한다")
    void publishPending_marksPublishFailedWhenKafkaSendFails() {
        UUID outboxId = UUID.randomUUID();
        NotificationFanoutOutbox outbox = outbox(outboxId);
        RuntimeException exception = new RuntimeException("kafka unavailable");
        given(claimService.claimBatch()).willReturn(List.of(outbox));
        given(kafkaTemplate.send(
                eq(NotificationKafkaTopics.FANOUT),
                eq(outboxId.toString()),
                any(NotificationFanoutKafkaMessage.class)
        )).willReturn(CompletableFuture.failedFuture(exception));

        relay.publishPending();

        then(statusService).should().markPublishFailed(outboxId, "kafka unavailable");
    }

    @Test
    @DisplayName("Kafka 발행 요청이 즉시 실패해도 실패 상태를 기록한다")
    void publishPending_marksPublishFailedWhenKafkaSendThrows() {
        UUID outboxId = UUID.randomUUID();
        NotificationFanoutOutbox outbox = outbox(outboxId);
        RuntimeException exception = new RuntimeException("serialization failed");
        given(claimService.claimBatch()).willReturn(List.of(outbox));
        given(kafkaTemplate.send(
                eq(NotificationKafkaTopics.FANOUT),
                eq(outboxId.toString()),
                any(NotificationFanoutKafkaMessage.class)
        )).willThrow(exception);

        relay.publishPending();

        then(statusService).should().markPublishFailed(outboxId, "serialization failed");
    }

    private NotificationFanoutOutbox outbox(UUID id) {
        NotificationFanoutOutbox outbox = new NotificationFanoutOutbox(new NotificationFanoutEvent(
                NotificationAudienceType.FOLLOWERS,
                UUID.randomUUID(),
                "시청 시작",
                "새로운 활동입니다.",
                NotificationLevel.INFO
        ));
        org.springframework.test.util.ReflectionTestUtils.setField(outbox, "id", id);
        return outbox;
    }
}
