package com.example.sb10_MoPl_team3.conversation.mapper;

import com.example.sb10_MoPl_team3.conversation.dto.response.ConversationDto;
import com.example.sb10_MoPl_team3.conversation.entity.Conversation;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.mapper.UserMapper;
import java.util.UUID;

public final class ConversationMapper {

    private ConversationMapper() {
    }

    public static ConversationDto toDto(Conversation conversation, UUID requestUserId) {
        User withUser = conversation.getUser1().getId().equals(requestUserId)
            ? conversation.getUser2()
            : conversation.getUser1();

        return new ConversationDto(
            conversation.getId(),
            UserMapper.toSummary(withUser),
            null,
            false
        );
    }
}
