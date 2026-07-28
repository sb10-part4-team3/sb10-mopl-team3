package com.example.sb10_MoPl_team3.notification.entity;

import com.example.sb10_MoPl_team3.global.base.BaseEntity;
import com.example.sb10_MoPl_team3.notification.enums.NotificationFanoutJobStatus;
import com.example.sb10_MoPl_team3.notification.enums.NotificationLevel;
import com.example.sb10_MoPl_team3.notification.event.NotificationAudienceType;
import com.example.sb10_MoPl_team3.notification.event.NotificationFanoutEvent;
import com.example.sb10_MoPl_team3.notification.kafka.NotificationFanoutKafkaMessage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "notification_fanout_jobs",
        indexes = {
                @Index(
                        name = "idx_notification_fanout_jobs_outbox_id",
                        columnList = "outbox_id",
                        unique = true
                ),
                @Index(
                        name = "idx_notification_fanout_jobs_status_updated_at",
                        columnList = "status, updated_at"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationFanoutJob extends BaseEntity {

    private static final int LAST_ERROR_MAX_LENGTH = 2_000;

    @Column(name = "outbox_id", nullable = false, unique = true)
    private UUID outboxId;

    @Enumerated(EnumType.STRING)
    @Column(name = "audience_type", nullable = false, length = 40)
    private NotificationAudienceType audienceType;

    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationLevel level;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationFanoutJobStatus status;

    @Column(name = "next_page", nullable = false)
    private int nextPage;

    @Column(name = "processed_count", nullable = false)
    private int processedCount;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "completed_at")
    private Instant completedAt;

    public NotificationFanoutJob(NotificationFanoutKafkaMessage message) {
        Objects.requireNonNull(message, "message는 필수입니다.");
        this.outboxId = message.outboxId();
        this.audienceType = message.audienceType();
        this.sourceId = message.sourceId();
        this.title = message.title();
        this.content = message.content();
        this.level = message.level();
        this.status = NotificationFanoutJobStatus.PROCESSING;
        this.nextPage = 0;
        this.processedCount = 0;
        this.retryCount = 0;
    }

    public NotificationFanoutEvent toEvent() {
        return new NotificationFanoutEvent(audienceType, sourceId, title, content, level);
    }

    public boolean isCompleted() {
        return status == NotificationFanoutJobStatus.COMPLETED;
    }

    public void markProcessing() {
        this.status = NotificationFanoutJobStatus.PROCESSING;
        this.lastError = null;
    }

    public void markPageProcessed(int page, int processedCountDelta) {
        if (page < nextPage) {
            return;
        }
        this.nextPage = page + 1;
        this.processedCount += processedCountDelta;
        this.status = NotificationFanoutJobStatus.PROCESSING;
    }

    public void markCompleted(Instant completedAt) {
        this.status = NotificationFanoutJobStatus.COMPLETED;
        this.completedAt = Objects.requireNonNull(completedAt, "completedAt은 필수입니다.");
        this.lastError = null;
    }

    public void markFailed(String errorMessage) {
        this.status = NotificationFanoutJobStatus.FAILED;
        this.retryCount++;
        this.lastError = truncate(errorMessage);
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
