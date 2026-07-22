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
import com.example.sb10_MoPl_team3.notification.enums.NotificationLevel;
import com.example.sb10_MoPl_team3.notification.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import com.example.sb10_MoPl_team3.user.mapper.UserResponseMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class DirectMessageAsyncService {

    private final DirectMessageRepository directMessageRepository;
    private final ConversationRepository conversationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final DirectMessageConversationPresence presence;
    private final UserResponseMapper userResponseMapper;

    @Async("directMessageExecutor")
    @Transactional
    public CompletableFuture<DirectMessageDto> saveAsync(
            UUID conversationId,
            UUID senderId,
            String content
    ) {
        Conversation conversation = conversationRepository.findWithUsersById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));
        MessageParticipants participants = resolveParticipants(conversation, senderId);

        DirectMessage directMessage = new DirectMessage(
                conversation,
                participants.sender(),
                participants.receiver(),
                content
        );
        boolean receiverActive = presence.isActive(participants.receiver().getId(), conversationId);
        if (receiverActive) {
            directMessage.markAsRead(Instant.now());
        }

        DirectMessage saved = directMessageRepository.saveAndFlush(directMessage);
        if (!receiverActive) {
            eventPublisher.publishEvent(new NotificationEvent(
                    participants.receiver().getId(),
                    "새 쪽지",
                    "%s님이 새로운 쪽지를 보냈습니다."
                            .formatted(participants.sender().getName()),
                    NotificationLevel.INFO
            ));
        }
        return CompletableFuture.completedFuture(DirectMessageMapper.toDto(saved, userResponseMapper));
    }

    private MessageParticipants resolveParticipants(Conversation conversation, UUID senderId) {
        if (conversation.getUser1().getId().equals(senderId)) {
            return new MessageParticipants(conversation.getUser1(), conversation.getUser2());
        }
        if (conversation.getUser2().getId().equals(senderId)) {
            return new MessageParticipants(conversation.getUser2(), conversation.getUser1());
        }
        throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }

    private record MessageParticipants(User sender, User receiver) {
    }
}
