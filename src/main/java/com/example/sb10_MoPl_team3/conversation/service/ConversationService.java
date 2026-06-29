package com.example.sb10_MoPl_team3.conversation.service;

import com.example.sb10_MoPl_team3.conversation.dto.request.ConversationCreateRequest;
import com.example.sb10_MoPl_team3.conversation.dto.request.ConversationFindAllRequest;
import com.example.sb10_MoPl_team3.conversation.dto.response.CursorResponseConversationDto;
import com.example.sb10_MoPl_team3.conversation.dto.response.ConversationDto;
import com.example.sb10_MoPl_team3.conversation.entity.Conversation;
import com.example.sb10_MoPl_team3.conversation.mapper.ConversationMapper;
import com.example.sb10_MoPl_team3.conversation.repository.ConversationRepository;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final PlatformTransactionManager transactionManager;

    @Transactional(readOnly = true)
    public CursorResponseConversationDto<ConversationDto> findAll(
        UUID requestUserId,
        ConversationFindAllRequest request
    ) {
        String sortBy = normalizeSortBy(request.sortBy());
        String sortDirection = normalizeSortDirection(request.sortDirection());
        boolean ascending = sortDirection.equals("ASCENDING");
        int limit = normalizeLimit(request.limit());
        Instant cursor = parseCursor(request.cursor(), request.idAfter());
        String keyword = normalizeKeyword(request.keywordLike());

        List<Conversation> fetchedConversations = new ArrayList<>(
            ascending
                ? conversationRepository.findParticipatingConversationsAsc(
                    requestUserId,
                    keyword,
                    cursor,
                    request.idAfter(),
                    PageRequest.of(0, limit + 1)
                )
                : conversationRepository.findParticipatingConversationsDesc(
                    requestUserId,
                    keyword,
                    cursor,
                    request.idAfter(),
                    PageRequest.of(0, limit + 1)
                )
        );

        boolean hasNext = fetchedConversations.size() > limit;
        List<Conversation> conversations = hasNext
            ? fetchedConversations.subList(0, limit)
            : fetchedConversations;

        String nextCursor = null;
        UUID nextIdAfter = null;

        if (hasNext && !conversations.isEmpty()) {
            Conversation lastConversation = conversations.get(conversations.size() - 1);
            nextCursor = lastConversation.getCreatedAt().toString();
            nextIdAfter = lastConversation.getId();
        }

        List<ConversationDto> data = conversations.stream()
            .map(conversation -> ConversationMapper.toDto(conversation, requestUserId))
            .toList();

        long totalCount = conversationRepository.countParticipatingConversations(
            requestUserId,
            keyword
        );

        return new CursorResponseConversationDto<>(
            data,
            nextCursor,
            nextIdAfter,
            hasNext,
            totalCount,
            sortBy,
            sortDirection
        );
    }

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

    private String normalizeSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank() || "createdAt".equalsIgnoreCase(sortBy)) {
            return "createdAt";
        }

        throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }

    private String normalizeSortDirection(String sortDirection) {
        if (sortDirection == null || sortDirection.isBlank()) {
            return "DESCENDING";
        }

        String normalized = sortDirection.toUpperCase(Locale.ROOT);
        if (!normalized.equals("ASCENDING") && !normalized.equals("DESCENDING")) {
            throw new BusinessException(ErrorCode.INVALID_SORT_DIRECTION);
        }

        return normalized;
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 20;
        }

        return Math.min(limit, 100);
    }

    private Instant parseCursor(String cursor, UUID idAfter) {
        boolean hasCursor = cursor != null && !cursor.isBlank();
        boolean hasIdAfter = idAfter != null;

        if (hasCursor != hasIdAfter) {
            throw new BusinessException(ErrorCode.INVALID_CURSOR);
        }

        if (!hasCursor) {
            return null;
        }

        try {
            return Instant.parse(cursor);
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.INVALID_CURSOR);
        }
    }

    private String normalizeKeyword(String keywordLike) {
        if (keywordLike == null || keywordLike.isBlank()) {
            return null;
        }

        return keywordLike.trim();
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
