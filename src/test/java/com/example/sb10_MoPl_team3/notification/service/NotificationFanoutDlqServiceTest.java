package com.example.sb10_MoPl_team3.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.sb10_MoPl_team3.notification.config.NotificationKafkaTopics;
import com.example.sb10_MoPl_team3.notification.entity.NotificationFanoutDlq;
import com.example.sb10_MoPl_team3.notification.enums.NotificationFanoutDlqStatus;
import com.example.sb10_MoPl_team3.notification.enums.NotificationLevel;
import com.example.sb10_MoPl_team3.notification.event.NotificationAudienceType;
import com.example.sb10_MoPl_team3.notification.kafka.NotificationFanoutKafkaMessage;
import com.example.sb10_MoPl_team3.notification.repository.NotificationFanoutDlqRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationFanoutDlqServiceTest {

    @Mock
    NotificationFanoutDlqRepository repository;

    @Mock
    KafkaTemplate<String, NotificationFanoutKafkaMessage> kafkaTemplate;

    Clock clock = Clock.fixed(Instant.parse("2026-07-14T00:00:00Z"), ZoneOffset.UTC);

    NotificationFanoutDlqService service;

    @BeforeEach
    void setUp() {
        service = new NotificationFanoutDlqService(repository, kafkaTemplate, clock);
    }

    @Test
    @DisplayName("DLQ 메시지를 PENDING 상태로 저장한다")
    void save_storesPendingDlq() {
        NotificationFanoutKafkaMessage message = message();
        given(repository.save(any(NotificationFanoutDlq.class)))
                .willAnswer(invocation -> {
                    NotificationFanoutDlq dlq = invocation.getArgument(0);
                    ReflectionTestUtils.setField(dlq, "id", UUID.randomUUID());
                    return dlq;
                });

        var result = service.save(message, "retry exhausted");

        assertThat(result.outboxId()).isEqualTo(message.outboxId());
        assertThat(result.status()).isEqualTo(NotificationFanoutDlqStatus.PENDING);
        assertThat(result.errorMessage()).isEqualTo("retry exhausted");
    }

    @Test
    @DisplayName("PENDING DLQ 목록을 조회한다")
    void findPending() {
        NotificationFanoutDlq dlq = dlq(message());
        given(repository.findByStatusOrderByCreatedAtDescIdDesc(
                eq(NotificationFanoutDlqStatus.PENDING),
                any(Pageable.class)))
                .willReturn(new SliceImpl<>(List.of(dlq)));

        var result = service.findPending(20);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(dlq.getId());
    }

    @Test
    @DisplayName("PENDING DLQ를 원 topic으로 재발행하고 RETRIED 상태로 변경한다")
    void retry_republishesAndMarksRetried() {
        NotificationFanoutKafkaMessage message = message();
        NotificationFanoutDlq dlq = dlq(message);
        given(repository.findById(dlq.getId())).willReturn(Optional.of(dlq));
        given(kafkaTemplate.send(
                eq(NotificationKafkaTopics.FANOUT),
                eq(message.outboxId().toString()),
                eq(message)))
                .willReturn(CompletableFuture.completedFuture(null));

        var result = service.retry(dlq.getId());

        then(kafkaTemplate).should().send(
                NotificationKafkaTopics.FANOUT,
                message.outboxId().toString(),
                message);
        assertThat(result.status()).isEqualTo(NotificationFanoutDlqStatus.RETRIED);
        assertThat(result.retriedAt()).isEqualTo(clock.instant());
    }

    @Test
    @DisplayName("DLQ 재발행 실패 시 RETRIED 상태로 변경하지 않는다")
    void retry_doesNotMarkRetriedWhenKafkaSendFails() {
        NotificationFanoutKafkaMessage message = message();
        NotificationFanoutDlq dlq = dlq(message);
        RuntimeException exception = new RuntimeException("kafka unavailable");
        given(repository.findById(dlq.getId())).willReturn(Optional.of(dlq));
        given(kafkaTemplate.send(
                eq(NotificationKafkaTopics.FANOUT),
                eq(message.outboxId().toString()),
                eq(message)))
                .willReturn(CompletableFuture.failedFuture(exception));

        assertThatThrownBy(() -> service.retry(dlq.getId()))
                .hasCause(exception);

        assertThat(dlq.getStatus()).isEqualTo(NotificationFanoutDlqStatus.PENDING);
    }

    @Test
    @DisplayName("이미 재처리된 DLQ는 다시 발행하지 않는다")
    void retry_skipsAlreadyRetriedDlq() {
        NotificationFanoutDlq dlq = dlq(message());
        dlq.markRetried(clock.instant());
        given(repository.findById(dlq.getId())).willReturn(Optional.of(dlq));

        var result = service.retry(dlq.getId());

        assertThat(result.status()).isEqualTo(NotificationFanoutDlqStatus.RETRIED);
        then(kafkaTemplate).should(never()).send(any(), any(), any());
    }

    private NotificationFanoutKafkaMessage message() {
        return new NotificationFanoutKafkaMessage(
                UUID.randomUUID(),
                NotificationAudienceType.FOLLOWERS,
                UUID.randomUUID(),
                "시청 시작",
                "새로운 활동입니다.",
                NotificationLevel.INFO
        );
    }

    private NotificationFanoutDlq dlq(NotificationFanoutKafkaMessage message) {
        NotificationFanoutDlq dlq = new NotificationFanoutDlq(message, "retry exhausted");
        ReflectionTestUtils.setField(dlq, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(dlq, "createdAt", clock.instant());
        return dlq;
    }
}
