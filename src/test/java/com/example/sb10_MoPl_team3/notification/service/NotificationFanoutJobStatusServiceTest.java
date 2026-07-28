package com.example.sb10_MoPl_team3.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.sb10_MoPl_team3.notification.entity.NotificationFanoutJob;
import com.example.sb10_MoPl_team3.notification.enums.NotificationFanoutJobStatus;
import com.example.sb10_MoPl_team3.notification.enums.NotificationLevel;
import com.example.sb10_MoPl_team3.notification.event.NotificationAudienceType;
import com.example.sb10_MoPl_team3.notification.kafka.NotificationFanoutKafkaMessage;
import com.example.sb10_MoPl_team3.notification.repository.NotificationFanoutJobRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationFanoutJobStatusServiceTest {

    @Mock
    NotificationFanoutJobRepository repository;

    @Test
    void start_savesNewJobWhenOutboxJobDoesNotExist() {
        NotificationFanoutKafkaMessage message = message();
        given(repository.findByOutboxId(message.outboxId())).willReturn(Optional.empty());
        given(repository.save(any(NotificationFanoutJob.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        NotificationFanoutJobStatusService service = new NotificationFanoutJobStatusService(repository);

        NotificationFanoutJob job = service.start(message);

        assertThat(job.getOutboxId()).isEqualTo(message.outboxId());
        assertThat(job.getStatus()).isEqualTo(NotificationFanoutJobStatus.PROCESSING);
        then(repository).should().save(any(NotificationFanoutJob.class));
    }

    @Test
    void start_marksExistingFailedJobProcessing() {
        NotificationFanoutKafkaMessage message = message();
        NotificationFanoutJob job = job(message);
        job.markFailed("failed");
        given(repository.findByOutboxId(message.outboxId())).willReturn(Optional.of(job));
        NotificationFanoutJobStatusService service = new NotificationFanoutJobStatusService(repository);

        NotificationFanoutJob result = service.start(message);

        assertThat(result).isSameAs(job);
        assertThat(result.getStatus()).isEqualTo(NotificationFanoutJobStatus.PROCESSING);
        assertThat(result.getLastError()).isNull();
        then(repository).should(never()).save(any());
    }

    @Test
    void start_keepsCompletedJobCompleted() {
        NotificationFanoutKafkaMessage message = message();
        NotificationFanoutJob job = job(message);
        job.markCompleted(Instant.parse("2026-07-14T00:00:00Z"));
        given(repository.findByOutboxId(message.outboxId())).willReturn(Optional.of(job));
        NotificationFanoutJobStatusService service = new NotificationFanoutJobStatusService(repository);

        NotificationFanoutJob result = service.start(message);

        assertThat(result.getStatus()).isEqualTo(NotificationFanoutJobStatus.COMPLETED);
    }

    @Test
    void markPageProcessed_updatesExistingJobProgress() {
        NotificationFanoutJob job = job(message());
        given(repository.findById(job.getId())).willReturn(Optional.of(job));
        NotificationFanoutJobStatusService service = new NotificationFanoutJobStatusService(repository);

        service.markPageProcessed(job.getId(), 2, 5);

        assertThat(job.getNextPage()).isEqualTo(3);
        assertThat(job.getProcessedCount()).isEqualTo(5);
    }

    @Test
    void markCompleted_updatesExistingJobCompletion() {
        NotificationFanoutJob job = job(message());
        Instant completedAt = Instant.parse("2026-07-14T00:00:00Z");
        given(repository.findById(job.getId())).willReturn(Optional.of(job));
        NotificationFanoutJobStatusService service = new NotificationFanoutJobStatusService(repository);

        service.markCompleted(job.getId(), completedAt);

        assertThat(job.getStatus()).isEqualTo(NotificationFanoutJobStatus.COMPLETED);
        assertThat(job.getCompletedAt()).isEqualTo(completedAt);
    }

    @Test
    void markFailed_updatesExistingJobFailure() {
        NotificationFanoutJob job = job(message());
        given(repository.findById(job.getId())).willReturn(Optional.of(job));
        NotificationFanoutJobStatusService service = new NotificationFanoutJobStatusService(repository);

        service.markFailed(job.getId(), "batch failed");

        assertThat(job.getStatus()).isEqualTo(NotificationFanoutJobStatus.FAILED);
        assertThat(job.getRetryCount()).isEqualTo(1);
        assertThat(job.getLastError()).isEqualTo("batch failed");
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

    private NotificationFanoutJob job(NotificationFanoutKafkaMessage message) {
        NotificationFanoutJob job = new NotificationFanoutJob(message);
        ReflectionTestUtils.setField(job, "id", UUID.randomUUID());
        return job;
    }
}
