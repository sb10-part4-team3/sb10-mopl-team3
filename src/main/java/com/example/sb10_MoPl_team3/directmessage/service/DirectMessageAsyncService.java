package com.example.sb10_MoPl_team3.directmessage.service;

import com.example.sb10_MoPl_team3.conversation.entity.Conversation;
import com.example.sb10_MoPl_team3.conversation.repository.ConversationRepository;
import com.example.sb10_MoPl_team3.directmessage.dto.DirectMessageDto;
import com.example.sb10_MoPl_team3.directmessage.entity.DirectMessage;
import com.example.sb10_MoPl_team3.directmessage.mapper.DirectMessageMapper;
import com.example.sb10_MoPl_team3.directmessage.repository.DirectMessageRepository;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class DirectMessageAsyncService {

    private final DirectMessageRepository directMessageRepository;
    private final ConversationRepository conversationRepository;

    @Async("directMessageExecutor")
    @Transactional
    public CompletableFuture<DirectMessageDto> saveAsync(
            UUID conversationId,
            UUID senderId,
            String content
    ) {
        Conversation conversation = conversationRepository.findWithUsersById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));
        User sender = findParticipant(conversation, senderId);
        User receiver = conversation.getUser1().getId().equals(senderId)
                ? conversation.getUser2()
                : conversation.getUser1();

        DirectMessage saved = directMessageRepository.saveAndFlush(
                new DirectMessage(conversation, sender, receiver, content));
        return CompletableFuture.completedFuture(DirectMessageMapper.toDto(saved));
    }

    private User findParticipant(Conversation conversation, UUID senderId) {
        if (conversation.getUser1().getId().equals(senderId)) {
            return conversation.getUser1();
        }
        if (conversation.getUser2().getId().equals(senderId)) {
            return conversation.getUser2();
        }
        throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }
}
