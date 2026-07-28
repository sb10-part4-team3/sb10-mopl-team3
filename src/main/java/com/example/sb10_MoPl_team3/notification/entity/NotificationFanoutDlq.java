package com.example.sb10_MoPl_team3.notification.entity;

import com.example.sb10_MoPl_team3.global.base.BaseEntity;
import com.example.sb10_MoPl_team3.notification.enums.NotificationFanoutDlqStatus;
import com.example.sb10_MoPl_team3.notification.enums.NotificationLevel;
import com.example.sb10_MoPl_team3.notification.event.NotificationAudienceType;
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
        name = "notification_fanout_dlq",
        indexes = {
                @Index(
                        name = "idx_notification_fanout_dlq_status_created_at_id",
                        columnList = "status, created_at, id"
                ),
                @Index(
                        name = "idx_notification_fanout_dlq_outbox_id",
                        columnList = "outbox_id"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationFanoutDlq extends BaseEntity {

    private static final int ERROR_MESSAGE_MAX_LENGTH = 2_000;

    @Column(name = "outbox_id", nullable = false)
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

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationFanoutDlqStatus status;

    @Column(name = "retried_at")
    private Instant retriedAt;

    public NotificationFanoutDlq(NotificationFanoutKafkaMessage message, String errorMessage) {
        Objects.requireNonNull(message, "message는 필수입니다.");
        this.outboxId = message.outboxId();
        this.audienceType = message.audienceType();
        this.sourceId = message.sourceId();
        this.title = message.title();
        this.content = message.content();
        this.level = message.level();
        this.errorMessage = truncate(errorMessage);
        this.status = NotificationFanoutDlqStatus.PENDING;
    }

    public NotificationFanoutKafkaMessage toMessage() {
        return new NotificationFanoutKafkaMessage(
                outboxId,
                audienceType,
                sourceId,
                title,
                content,
                level
        );
    }

    public void markRetried(Instant retriedAt) {
        this.status = NotificationFanoutDlqStatus.RETRIED;
        this.retriedAt = Objects.requireNonNull(retriedAt, "retriedAt은 필수입니다.");
    }

    private String truncate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= ERROR_MESSAGE_MAX_LENGTH
                ? value
                : value.substring(0, ERROR_MESSAGE_MAX_LENGTH);
    }
}
