package com.example.sb10_MoPl_team3.directmessage.dto;

import java.time.Instant;
import java.util.UUID;

public record DirectMessageReadStatusChange(
        UUID conversationId,
        UUID directMessageId,
        UUID readerId,
        boolean read,
        Instant readAt
) {
}
