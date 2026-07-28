package com.example.sb10_MoPl_team3.directmessage.dto;

import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;
import java.time.Instant;
import java.util.UUID;

public record DirectMessageDto(
    UUID id,
    UUID conversationId,
    Instant createdAt,
    UserSummary sender,
    UserSummary receiver,
    String content
) {
}
