package com.example.sb10_MoPl_team3.notification.service;

import com.example.sb10_MoPl_team3.notification.enums.NotificationFanoutOutboxStatus;
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

    private static final int LAST_ERROR_MAX_LENGTH = 2_000;

    private final NotificationFanoutOutboxRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPublished(UUID outboxId, Instant publishedAt) {
        repository.updatePublished(
                outboxId,
                NotificationFanoutOutboxStatus.PUBLISHED,
                publishedAt);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPublishFailed(UUID outboxId, String errorMessage) {
        repository.updatePublishFailed(
                outboxId,
                NotificationFanoutOutboxStatus.PUBLISH_FAILED,
                truncate(errorMessage));
    }

    private String truncate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= LAST_ERROR_MAX_LENGTH
                ? value
                : value.substring(0, LAST_ERROR_MAX_LENGTH);
    }
}
