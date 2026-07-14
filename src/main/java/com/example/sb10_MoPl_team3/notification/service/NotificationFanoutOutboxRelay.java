package com.example.sb10_MoPl_team3.notification.service;

import com.example.sb10_MoPl_team3.notification.config.NotificationKafkaTopics;
import com.example.sb10_MoPl_team3.notification.entity.NotificationFanoutOutbox;
import com.example.sb10_MoPl_team3.notification.enums.NotificationFanoutOutboxStatus;
import com.example.sb10_MoPl_team3.notification.kafka.NotificationFanoutKafkaMessage;
import com.example.sb10_MoPl_team3.notification.repository.NotificationFanoutOutboxRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationFanoutOutboxRelay {

    private static final List<NotificationFanoutOutboxStatus> RELAY_TARGET_STATUSES = List.of(
            NotificationFanoutOutboxStatus.PENDING,
            NotificationFanoutOutboxStatus.PUBLISH_FAILED
    );

    private final NotificationFanoutOutboxRepository repository;
    private final NotificationFanoutOutboxStatusService statusService;
    private final KafkaTemplate<String, NotificationFanoutKafkaMessage> kafkaTemplate;
    private final Clock clock;

    @Value("${notification.kafka.outbox-relay.batch-size:50}")
    private int batchSize;

    @Value("${notification.kafka.outbox-relay.processing-timeout-seconds:300}")
    private long processingTimeoutSeconds;

    @Scheduled(
            fixedDelayString = "${notification.kafka.outbox-relay.fixed-delay:5000}",
            initialDelayString = "${notification.kafka.outbox-relay.initial-delay:5000}"
    )
    public void publishPending() {
        claimBatch().forEach(this::publish);
    }

    @Transactional
    public List<NotificationFanoutOutbox> claimBatch() {
        recoverStaleProcessing();
        var page = PageRequest.of(0, batchSize);
        List<NotificationFanoutOutbox> outboxes = repository
                .findByStatusInOrderByCreatedAtAscIdAsc(RELAY_TARGET_STATUSES, page)
                .getContent();
        outboxes.forEach(NotificationFanoutOutbox::markProcessing);
        return outboxes;
    }

    private void recoverStaleProcessing() {
        Instant staleBefore = clock.instant().minusSeconds(processingTimeoutSeconds);
        int recoveredCount = repository.resetStaleProcessing(
                NotificationFanoutOutboxStatus.PROCESSING,
                NotificationFanoutOutboxStatus.PENDING,
                staleBefore);
        if (recoveredCount > 0) {
            log.warn("오래된 PROCESSING 알림 팬아웃 outbox 복구: count={}, staleBefore={}",
                    recoveredCount,
                    staleBefore);
        }
    }

    private void publish(NotificationFanoutOutbox outbox) {
        NotificationFanoutKafkaMessage message = NotificationFanoutKafkaMessage.from(outbox);
        UUID outboxId = outbox.getId();
        try {
            kafkaTemplate
                    .send(NotificationKafkaTopics.FANOUT, outboxId.toString(), message)
                    .whenComplete((result, exception) -> {
                        if (exception == null) {
                            statusService.markPublished(outboxId, clock.instant());
                            log.info("알림 팬아웃 outbox Kafka 발행 완료: outboxId={}", outboxId);
                            return;
                        }
                        statusService.markPublishFailed(outboxId, exception.getMessage());
                        log.error("알림 팬아웃 outbox Kafka 발행 실패: outboxId={}", outboxId, exception);
                    });
        } catch (RuntimeException exception) {
            statusService.markPublishFailed(outboxId, exception.getMessage());
            log.error("알림 팬아웃 outbox Kafka 발행 요청 실패: outboxId={}", outboxId, exception);
        }
    }
}
