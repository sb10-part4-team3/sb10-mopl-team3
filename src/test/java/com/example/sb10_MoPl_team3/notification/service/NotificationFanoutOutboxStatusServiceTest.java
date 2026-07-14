package com.example.sb10_MoPl_team3.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

import com.example.sb10_MoPl_team3.notification.enums.NotificationFanoutOutboxStatus;
import com.example.sb10_MoPl_team3.notification.repository.NotificationFanoutOutboxRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationFanoutOutboxStatusServiceTest {

    @Mock
    NotificationFanoutOutboxRepository repository;

    @Test
    void markPublished_updatesStatusWithPublishedAt() {
        UUID outboxId = UUID.randomUUID();
        Instant publishedAt = Instant.parse("2026-07-14T00:00:00Z");
        NotificationFanoutOutboxStatusService service =
                new NotificationFanoutOutboxStatusService(repository);

        service.markPublished(outboxId, publishedAt);

        then(repository).should().updatePublished(
                outboxId,
                NotificationFanoutOutboxStatus.PUBLISHED,
                publishedAt);
    }

    @Test
    void markPublishFailed_updatesStatusWithTruncatedErrorMessage() {
        UUID outboxId = UUID.randomUUID();
        String longMessage = "x".repeat(2_100);
        NotificationFanoutOutboxStatusService service =
                new NotificationFanoutOutboxStatusService(repository);

        service.markPublishFailed(outboxId, longMessage);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        then(repository).should().updatePublishFailed(
                org.mockito.ArgumentMatchers.eq(outboxId),
                org.mockito.ArgumentMatchers.eq(NotificationFanoutOutboxStatus.PUBLISH_FAILED),
                messageCaptor.capture());
        assertThat(messageCaptor.getValue()).hasSize(2_000);
    }

    @Test
    void markPublishFailed_normalizesBlankErrorMessageToNull() {
        UUID outboxId = UUID.randomUUID();
        NotificationFanoutOutboxStatusService service =
                new NotificationFanoutOutboxStatusService(repository);

        service.markPublishFailed(outboxId, "   ");

        then(repository).should().updatePublishFailed(
                outboxId,
                NotificationFanoutOutboxStatus.PUBLISH_FAILED,
                null);
    }
}
