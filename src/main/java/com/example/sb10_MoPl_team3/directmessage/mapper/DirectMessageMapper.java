package com.example.sb10_MoPl_team3.directmessage.mapper;

import com.example.sb10_MoPl_team3.directmessage.dto.DirectMessageDto;
import com.example.sb10_MoPl_team3.directmessage.entity.DirectMessage;
import com.example.sb10_MoPl_team3.user.mapper.UserResponseMapper;

public final class DirectMessageMapper {

    private DirectMessageMapper() {
    }

    public static DirectMessageDto toDto(
        DirectMessage directMessage,
        UserResponseMapper userResponseMapper
    ) {
        return new DirectMessageDto(
            directMessage.getId(),
            directMessage.getConversation().getId(),
            directMessage.getCreatedAt(),
            userResponseMapper.toSummary(directMessage.getSender()),
            userResponseMapper.toSummary(directMessage.getReceiver()),
            directMessage.getContent()
        );
    }
}
