package com.example.sb10_MoPl_team3.conversation.service;

import com.example.sb10_MoPl_team3.conversation.dto.request.ConversationCreateRequest;
import com.example.sb10_MoPl_team3.conversation.dto.response.ConversationDto;
import com.example.sb10_MoPl_team3.conversation.entity.Conversation;
import com.example.sb10_MoPl_team3.conversation.mapper.ConversationMapper;
import com.example.sb10_MoPl_team3.conversation.repository.ConversationRepository;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final PlatformTransactionManager transactionManager;

    public ConversationDto create(UUID requestUserId, ConversationCreateRequest request) {
        UUID withUserId = request.withUserId();

        if (requestUserId.equals(withUserId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return findConversation(requestUserId, withUserId)
            .orElseGet(() -> createOrFindConversation(requestUserId, withUserId));
    }

    public ConversationDto findWithUser(UUID requestUserId, UUID userId) {
        if (requestUserId.equals(userId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return findConversation(requestUserId, userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));
    }

    public ConversationDto find(UUID requestUserId, UUID conversationId) {
        Conversation conversation = conversationRepository.findWithUsersById(conversationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));

        if (!isParticipant(conversation, requestUserId)) {
            throw new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND);
        }

        return ConversationMapper.toDto(conversation, requestUserId);
    }

    private ConversationDto createOrFindConversation(UUID requestUserId, UUID withUserId) {
        try {
            return createNewConversation(requestUserId, withUserId);
        } catch (DataIntegrityViolationException exception) {
            return findConversation(requestUserId, withUserId)
                .orElseThrow(() -> exception);
        }
    }

    private Optional<ConversationDto> findConversation(
        UUID requestUserId,
        UUID withUserId
    ) {
        return conversationRepository.findByUserIds(requestUserId, withUserId)
            .map(conversation -> ConversationMapper.toDto(conversation, requestUserId));
    }

    private boolean isParticipant(Conversation conversation, UUID userId) {
        return conversation.getUser1().getId().equals(userId)
            || conversation.getUser2().getId().equals(userId);
    }

    private ConversationDto createNewConversation(UUID requestUserId, UUID withUserId) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        return transactionTemplate.execute(status -> {
            User requestUser = findUser(requestUserId);
            User withUser = findUser(withUserId);

            Conversation conversation = conversationRepository.saveAndFlush(
                new Conversation(requestUser, withUser)
            );
            return ConversationMapper.toDto(conversation, requestUserId);
        });
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
