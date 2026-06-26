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
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    public ConversationDto create(UUID requestUserId, ConversationCreateRequest request) {
        UUID withUserId = request.withUserId();

        if (requestUserId.equals(withUserId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return conversationRepository.findByUserIds(requestUserId, withUserId)
            .map(conversation -> ConversationMapper.toDto(conversation, requestUserId))
            .orElseGet(() -> createNewConversation(requestUserId, withUserId));
    }

    private ConversationDto createNewConversation(UUID requestUserId, UUID withUserId) {
        User requestUser = findUser(requestUserId);
        User withUser = findUser(withUserId);

        Conversation conversation = conversationRepository.save(
            new Conversation(requestUser, withUser)
        );
        return ConversationMapper.toDto(conversation, requestUserId);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
