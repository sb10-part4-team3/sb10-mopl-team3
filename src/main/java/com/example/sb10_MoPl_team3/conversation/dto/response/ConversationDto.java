package com.example.sb10_MoPl_team3.conversation.dto.response;

import com.example.sb10_MoPl_team3.directmessage.dto.DirectMessageDto;
import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;
import java.util.UUID;

public record ConversationDto(
    UUID id,
    UserSummary with,
    DirectMessageDto lastestMessage,
    boolean hasUnread
) {
}
