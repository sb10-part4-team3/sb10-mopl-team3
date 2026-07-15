package com.example.sb10_MoPl_team3.notification.service;

import com.example.sb10_MoPl_team3.notification.entity.NotificationFanoutOutbox;
import com.example.sb10_MoPl_team3.notification.enums.NotificationFanoutOutboxStatus;
import com.example.sb10_MoPl_team3.notification.repository.NotificationFanoutOutboxRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationFanoutOutboxClaimService {

    private static final List<NotificationFanoutOutboxStatus> RELAY_TARGET_STATUSES = List.of(
            NotificationFanoutOutboxStatus.PENDING,
            NotificationFanoutOutboxStatus.PUBLISH_FAILED
    );

    private final NotificationFanoutOutboxRepository repository;
    private final Clock clock;

    @Value("${notification.kafka.outbox-relay.batch-size:50}")
    private int batchSize;

    @Value("${notification.kafka.outbox-relay.processing-timeout-seconds:300}")
    private long processingTimeoutSeconds;

    @Transactional
    public List<NotificationFanoutOutbox> claimBatch() {
        recoverStaleProcessing();
        List<String> statuses = RELAY_TARGET_STATUSES.stream()
                .map(Enum::name)
                .toList();
        List<NotificationFanoutOutbox> outboxes = repository.findClaimTargets(statuses, batchSize);
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
}
