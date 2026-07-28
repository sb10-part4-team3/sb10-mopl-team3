package com.example.sb10_MoPl_team3.notification.dto;

import com.example.sb10_MoPl_team3.notification.entity.NotificationFanoutDlq;
import com.example.sb10_MoPl_team3.notification.enums.NotificationFanoutDlqStatus;
import com.example.sb10_MoPl_team3.notification.enums.NotificationLevel;
import com.example.sb10_MoPl_team3.notification.event.NotificationAudienceType;
import java.time.Instant;
import java.util.UUID;

public record NotificationFanoutDlqDto(
        UUID id,
        UUID outboxId,
        NotificationAudienceType audienceType,
        UUID sourceId,
        String title,
        String content,
        NotificationLevel level,
        String errorMessage,
        NotificationFanoutDlqStatus status,
        Instant createdAt,
        Instant retriedAt
) {

    public static NotificationFanoutDlqDto from(NotificationFanoutDlq dlq) {
        return new NotificationFanoutDlqDto(
                dlq.getId(),
                dlq.getOutboxId(),
                dlq.getAudienceType(),
                dlq.getSourceId(),
                dlq.getTitle(),
                dlq.getContent(),
                dlq.getLevel(),
                dlq.getErrorMessage(),
                dlq.getStatus(),
                dlq.getCreatedAt(),
                dlq.getRetriedAt()
        );
    }
}
