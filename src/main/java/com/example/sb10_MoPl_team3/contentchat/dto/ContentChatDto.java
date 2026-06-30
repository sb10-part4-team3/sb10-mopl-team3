package com.example.sb10_MoPl_team3.contentchat.dto;

import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;

import java.time.Instant;
import java.util.UUID;

public record ContentChatDto(
        UUID id,
        UUID contentId,
        Instant createdAt,
        UserSummary sender,
        String content
) {
}
