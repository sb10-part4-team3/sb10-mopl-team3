package com.example.sb10_MoPl_team3.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.sb10_MoPl_team3.notification.entity.NotificationFanoutOutbox;
import com.example.sb10_MoPl_team3.notification.enums.NotificationFanoutOutboxStatus;
import com.example.sb10_MoPl_team3.notification.enums.NotificationLevel;
import com.example.sb10_MoPl_team3.notification.event.NotificationAudienceType;
import com.example.sb10_MoPl_team3.notification.event.NotificationFanoutEvent;
import com.example.sb10_MoPl_team3.notification.repository.NotificationFanoutOutboxRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationFanoutOutboxClaimServiceTest {

    @Mock
    NotificationFanoutOutboxRepository repository;

    Clock clock = Clock.fixed(Instant.parse("2026-07-09T00:00:00Z"), ZoneOffset.UTC);

    NotificationFanoutOutboxClaimService service;

    @BeforeEach
    void setUp() {
        service = new NotificationFanoutOutboxClaimService(repository, clock);
        ReflectionTestUtils.setField(service, "batchSize", 50);
        ReflectionTestUtils.setField(service, "processingTimeoutSeconds", 300L);
    }

    @Test
    @DisplayName("오래된 PROCESSING outbox를 복구하고 발행 대상 outbox를 PROCESSING 상태로 claim한다")
    void claimBatch_recoversStaleProcessingAndMarksOutboxesProcessing() {
        NotificationFanoutOutbox outbox = outbox(UUID.randomUUID());
        given(repository.findClaimTargets(any(), eq(50))).willReturn(List.of(outbox));

        var claimed = service.claimBatch();

        assertThat(claimed).containsExactly(outbox);
        assertThat(outbox.getStatus()).isEqualTo(NotificationFanoutOutboxStatus.PROCESSING);
        then(repository).should().resetStaleProcessing(
                NotificationFanoutOutboxStatus.PROCESSING,
                NotificationFanoutOutboxStatus.PENDING,
                clock.instant().minusSeconds(300));
        then(repository).should().findClaimTargets(
                List.of("PENDING", "PUBLISH_FAILED"),
                50);
    }

    private NotificationFanoutOutbox outbox(UUID id) {
        NotificationFanoutOutbox outbox = new NotificationFanoutOutbox(new NotificationFanoutEvent(
                NotificationAudienceType.FOLLOWERS,
                UUID.randomUUID(),
                "시청 시작",
                "새로운 활동입니다.",
                NotificationLevel.INFO
        ));
        ReflectionTestUtils.setField(outbox, "id", id);
        return outbox;
    }
}
