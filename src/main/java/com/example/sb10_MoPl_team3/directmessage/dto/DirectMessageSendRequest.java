package com.example.sb10_MoPl_team3.directmessage.dto;

import jakarta.validation.constraints.NotBlank;

public record DirectMessageSendRequest(
        @NotBlank(message = "쪽지 내용은 필수입니다.")
        String content
) {
}
