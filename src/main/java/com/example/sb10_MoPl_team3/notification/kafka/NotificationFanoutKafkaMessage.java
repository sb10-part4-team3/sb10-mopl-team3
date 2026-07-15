package com.example.sb10_MoPl_team3.notification.kafka;

import com.example.sb10_MoPl_team3.notification.entity.NotificationFanoutOutbox;
import com.example.sb10_MoPl_team3.notification.enums.NotificationLevel;
import com.example.sb10_MoPl_team3.notification.event.NotificationAudienceType;
import com.example.sb10_MoPl_team3.notification.event.NotificationFanoutEvent;
import java.util.Objects;
import java.util.UUID;

public record NotificationFanoutKafkaMessage(
        UUID outboxId,
        NotificationAudienceType audienceType,
        UUID sourceId,
        String title,
        String content,
        NotificationLevel level
) {

    public NotificationFanoutKafkaMessage {
        Objects.requireNonNull(outboxId, "outboxId는 필수입니다.");
        Objects.requireNonNull(audienceType, "audienceType은 필수입니다.");
        Objects.requireNonNull(sourceId, "sourceId는 필수입니다.");
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title은 필수입니다.");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content는 필수입니다.");
        }
        Objects.requireNonNull(level, "level은 필수입니다.");
    }

    public static NotificationFanoutKafkaMessage from(NotificationFanoutOutbox outbox) {
        return new NotificationFanoutKafkaMessage(
                outbox.getId(),
                outbox.getAudienceType(),
                outbox.getSourceId(),
                outbox.getTitle(),
                outbox.getContent(),
                outbox.getLevel()
        );
    }

    public NotificationFanoutEvent toEvent() {
        return new NotificationFanoutEvent(audienceType, sourceId, title, content, level);
    }
}
