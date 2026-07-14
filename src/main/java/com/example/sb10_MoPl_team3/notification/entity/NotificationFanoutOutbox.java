package com.example.sb10_MoPl_team3.notification.entity;

import com.example.sb10_MoPl_team3.global.base.BaseEntity;
import com.example.sb10_MoPl_team3.notification.enums.NotificationFanoutOutboxStatus;
import com.example.sb10_MoPl_team3.notification.enums.NotificationLevel;
import com.example.sb10_MoPl_team3.notification.event.NotificationAudienceType;
import com.example.sb10_MoPl_team3.notification.event.NotificationFanoutEvent;
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
        name = "notification_fanout_outbox",
        indexes = {
                @Index(
                        name = "idx_notification_fanout_outbox_status_created_at_id",
                        columnList = "status, created_at, id"
                ),
                @Index(
                        name = "idx_notification_fanout_outbox_source",
                        columnList = "audience_type, source_id"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationFanoutOutbox extends BaseEntity {

    private static final int LAST_ERROR_MAX_LENGTH = 2_000;

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
    private NotificationFanoutOutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "published_at")
    private Instant publishedAt;

    public NotificationFanoutOutbox(NotificationFanoutEvent event) {
        Objects.requireNonNull(event, "event는 필수입니다.");
        this.audienceType = event.audienceType();
        this.sourceId = event.sourceId();
        this.title = event.title();
        this.content = event.content();
        this.level = event.level();
        this.status = NotificationFanoutOutboxStatus.PENDING;
        this.retryCount = 0;
    }

    public NotificationFanoutEvent toEvent() {
        return new NotificationFanoutEvent(audienceType, sourceId, title, content, level);
    }

    public void markProcessing() {
        this.status = NotificationFanoutOutboxStatus.PROCESSING;
    }

    public void markPublished(Instant publishedAt) {
        this.status = NotificationFanoutOutboxStatus.PUBLISHED;
        this.publishedAt = Objects.requireNonNull(publishedAt, "publishedAt은 필수입니다.");
        this.lastError = null;
    }

    public void markPublishFailed(String errorMessage) {
        this.status = NotificationFanoutOutboxStatus.PUBLISH_FAILED;
        this.retryCount++;
        this.lastError = truncate(errorMessage);
    }

    public void markPending() {
        this.status = NotificationFanoutOutboxStatus.PENDING;
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
