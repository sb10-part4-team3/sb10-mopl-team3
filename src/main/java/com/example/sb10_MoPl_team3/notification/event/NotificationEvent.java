package com.example.sb10_MoPl_team3.notification.event;

import com.example.sb10_MoPl_team3.notification.enums.NotificationLevel;
import java.util.Objects;
import java.util.UUID;

public record NotificationEvent(
        UUID receiverId,
        String title,
        String content,
        NotificationLevel level
) {

    public NotificationEvent {
        Objects.requireNonNull(receiverId, "receiverId는 필수입니다.");
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title은 필수입니다.");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content는 필수입니다.");
        }
        Objects.requireNonNull(level, "level은 필수입니다.");
    }
}
