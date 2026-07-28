package com.example.sb10_MoPl_team3.contentchat.dto;

import jakarta.validation.constraints.NotBlank;

public record ContentChatSendRequest(
        @NotBlank(message = "채팅 내용은 필수입니다.")
        String content
) {
}
