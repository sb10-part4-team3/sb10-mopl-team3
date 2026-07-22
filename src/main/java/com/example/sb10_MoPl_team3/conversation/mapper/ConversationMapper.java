package com.example.sb10_MoPl_team3.conversation.mapper;

import com.example.sb10_MoPl_team3.conversation.dto.response.ConversationDto;
import com.example.sb10_MoPl_team3.conversation.entity.Conversation;
import com.example.sb10_MoPl_team3.directmessage.dto.DirectMessageDto;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.mapper.UserResponseMapper;
import java.util.UUID;

public final class ConversationMapper {

    private ConversationMapper() {
    }

    public static ConversationDto toDto(
        Conversation conversation,
        UUID requestUserId,
        UserResponseMapper userResponseMapper
    ) {
        return toDto(conversation, requestUserId, null, false, userResponseMapper);
    }

    public static ConversationDto toDto(
        Conversation conversation,
        UUID requestUserId,
        DirectMessageDto latestMessage,
        boolean hasUnread,
        UserResponseMapper userResponseMapper
    ) {
        User withUser = conversation.getUser1().getId().equals(requestUserId)
            ? conversation.getUser2()
            : conversation.getUser1();

        return new ConversationDto(
            conversation.getId(),
            userResponseMapper.toSummary(withUser),
            latestMessage,
            hasUnread
        );
    }
}
