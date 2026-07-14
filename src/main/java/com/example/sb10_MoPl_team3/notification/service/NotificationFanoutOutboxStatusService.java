package com.example.sb10_MoPl_team3.notification.service;

import com.example.sb10_MoPl_team3.notification.repository.NotificationFanoutOutboxRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationFanoutOutboxStatusService {

    private final NotificationFanoutOutboxRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPublished(UUID outboxId, Instant publishedAt) {
        repository.findById(outboxId)
                .ifPresent(outbox -> outbox.markPublished(publishedAt));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPublishFailed(UUID outboxId, String errorMessage) {
        repository.findById(outboxId)
                .ifPresent(outbox -> outbox.markPublishFailed(errorMessage));
    }
}
