package com.example.sb10_MoPl_team3.notification.service;

import com.example.sb10_MoPl_team3.notification.entity.NotificationFanoutJob;
import com.example.sb10_MoPl_team3.notification.kafka.NotificationFanoutKafkaMessage;
import com.example.sb10_MoPl_team3.notification.repository.NotificationFanoutJobRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationFanoutJobStatusService {

    private final NotificationFanoutJobRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationFanoutJob start(NotificationFanoutKafkaMessage message) {
        NotificationFanoutJob job = repository.findByOutboxId(message.outboxId())
                .orElseGet(() -> repository.save(new NotificationFanoutJob(message)));
        if (!job.isCompleted()) {
            job.markProcessing();
        }
        return job;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPageProcessed(UUID jobId, int page, int processedCountDelta) {
        repository.findById(jobId)
                .ifPresent(job -> job.markPageProcessed(page, processedCountDelta));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(UUID jobId, Instant completedAt) {
        repository.findById(jobId)
                .ifPresent(job -> job.markCompleted(completedAt));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID jobId, String errorMessage) {
        repository.findById(jobId)
                .ifPresent(job -> job.markFailed(errorMessage));
    }
}
