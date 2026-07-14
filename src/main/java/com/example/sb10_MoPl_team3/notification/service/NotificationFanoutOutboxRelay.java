package com.example.sb10_MoPl_team3.notification.service;

import com.example.sb10_MoPl_team3.notification.config.NotificationKafkaTopics;
import com.example.sb10_MoPl_team3.notification.entity.NotificationFanoutOutbox;
import com.example.sb10_MoPl_team3.notification.kafka.NotificationFanoutKafkaMessage;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationFanoutOutboxRelay {

    private final NotificationFanoutOutboxClaimService claimService;
    private final NotificationFanoutOutboxStatusService statusService;
    private final KafkaTemplate<String, NotificationFanoutKafkaMessage> kafkaTemplate;
    private final Clock clock;

    @Scheduled(
            fixedDelayString = "${notification.kafka.outbox-relay.fixed-delay:5000}",
            initialDelayString = "${notification.kafka.outbox-relay.initial-delay:5000}"
    )
    public void publishPending() {
        claimService.claimBatch().forEach(this::publish);
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
