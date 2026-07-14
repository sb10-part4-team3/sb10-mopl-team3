package com.example.sb10_MoPl_team3.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sb10_MoPl_team3.global.config.JpaAuditingConfig;
import com.example.sb10_MoPl_team3.global.config.QuerydslConfig;
import com.example.sb10_MoPl_team3.notification.entity.NotificationFanoutJob;
import com.example.sb10_MoPl_team3.notification.enums.NotificationFanoutJobStatus;
import com.example.sb10_MoPl_team3.notification.enums.NotificationLevel;
import com.example.sb10_MoPl_team3.notification.event.NotificationAudienceType;
import com.example.sb10_MoPl_team3.notification.kafka.NotificationFanoutKafkaMessage;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
class NotificationFanoutJobRepositoryTest {

    @Autowired
    NotificationFanoutJobRepository repository;

    @Test
    @DisplayName("outboxId로 팬아웃 job을 조회한다")
    void findByOutboxId() {
        NotificationFanoutKafkaMessage message = message();
        NotificationFanoutJob saved = repository.saveAndFlush(new NotificationFanoutJob(message));

        var result = repository.findByOutboxId(message.outboxId());

        assertThat(result).contains(saved);
        assertThat(result.get().toEvent()).isEqualTo(message.toEvent());
    }

    @Test
    @DisplayName("페이지 진행, 실패, 완료 상태를 저장한다")
    void updateJobProgress() {
        NotificationFanoutJob job = repository.saveAndFlush(
                new NotificationFanoutJob(message()));

        job.markPageProcessed(0, 100);
        job.markFailed("batch failed");
        repository.saveAndFlush(job);

        NotificationFanoutJob failed = repository.findById(job.getId()).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(NotificationFanoutJobStatus.FAILED);
        assertThat(failed.getNextPage()).isEqualTo(1);
        assertThat(failed.getProcessedCount()).isEqualTo(100);
        assertThat(failed.getRetryCount()).isEqualTo(1);
        assertThat(failed.getLastError()).isEqualTo("batch failed");

        Instant completedAt = Instant.parse("2026-07-09T00:00:00Z");
        failed.markCompleted(completedAt);
        repository.saveAndFlush(failed);

        NotificationFanoutJob completed = repository.findById(job.getId()).orElseThrow();
        assertThat(completed.getStatus()).isEqualTo(NotificationFanoutJobStatus.COMPLETED);
        assertThat(completed.getCompletedAt()).isEqualTo(completedAt);
        assertThat(completed.getLastError()).isNull();
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
}
