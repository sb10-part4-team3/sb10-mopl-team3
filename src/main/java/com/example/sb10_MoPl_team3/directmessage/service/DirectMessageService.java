package com.example.sb10_MoPl_team3.directmessage.service;

import com.example.sb10_MoPl_team3.conversation.entity.Conversation;
import com.example.sb10_MoPl_team3.conversation.repository.ConversationRepository;
import com.example.sb10_MoPl_team3.directmessage.dto.DirectMessageDto;
import com.example.sb10_MoPl_team3.directmessage.dto.DirectMessageReadStatusChange;
import com.example.sb10_MoPl_team3.directmessage.dto.response.CursorResponseDirectMessageDto;
import com.example.sb10_MoPl_team3.directmessage.entity.DirectMessage;
import com.example.sb10_MoPl_team3.directmessage.mapper.DirectMessageMapper;
import com.example.sb10_MoPl_team3.directmessage.repository.DirectMessageRepository;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DirectMessageService {

    private final DirectMessageRepository directMessageRepository;
    private final ConversationRepository conversationRepository;

    @Transactional
    public DirectMessageReadStatusChange read(
            UUID requestUserId,
            UUID conversationId,
            UUID directMessageId
    ) {
        Conversation conversation = conversationRepository.findWithUsersById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));
        if (!isParticipant(conversation, requestUserId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        DirectMessage directMessage = directMessageRepository
                .findByIdAndConversationId(directMessageId, conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DIRECT_MESSAGE_NOT_FOUND));
        if (!directMessage.getReceiver().getId().equals(requestUserId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        directMessage.markAsRead(Instant.now());
        return new DirectMessageReadStatusChange(
                conversationId,
                directMessageId,
                requestUserId,
                directMessage.isRead(),
                directMessage.getReadAt()
        );
    }

    @Transactional(readOnly = true)
    public CursorResponseDirectMessageDto<DirectMessageDto> findAll(
        UUID requestUserId,
        UUID conversationId,
        String cursor,
        UUID idAfter,
        int limit,
        String sortDirection,
        String sortBy
    ) {
        Conversation conversation = conversationRepository.findWithUsersById(conversationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));

        if (!isParticipant(conversation, requestUserId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        String normalizedSortBy = normalizeSortBy(sortBy);
        String normalizedSortDirection = normalizeSortDirection(sortDirection);
        boolean ascending = normalizedSortDirection.equals("ASCENDING");
        int normalizedLimit = normalizeLimit(limit);
        Instant parsedCursor = parseCursor(cursor, idAfter);

        List<DirectMessage> fetchedMessages = new ArrayList<>(
            ascending
                ? directMessageRepository.findByConversationIdWithCursorAsc(
                    conversationId,
                    parsedCursor,
                    idAfter,
                    PageRequest.of(0, normalizedLimit + 1)
                )
                : directMessageRepository.findByConversationIdWithCursorDesc(
                    conversationId,
                    parsedCursor,
                    idAfter,
                    PageRequest.of(0, normalizedLimit + 1)
                )
        );

        boolean hasNext = fetchedMessages.size() > normalizedLimit;
        List<DirectMessage> messages = hasNext
            ? fetchedMessages.subList(0, normalizedLimit)
            : fetchedMessages;

        String nextCursor = null;
        UUID nextIdAfter = null;

        if (hasNext && !messages.isEmpty()) {
            DirectMessage lastMessage = messages.get(messages.size() - 1);
            nextCursor = lastMessage.getCreatedAt().toString();
            nextIdAfter = lastMessage.getId();
        }

        List<DirectMessageDto> data = messages.stream()
            .map(DirectMessageMapper::toDto)
            .toList();

        long totalCount = directMessageRepository.countByConversationId(conversationId);

        return new CursorResponseDirectMessageDto<>(
            data,
            nextCursor,
            nextIdAfter,
            hasNext,
            totalCount,
            normalizedSortBy,
            normalizedSortDirection
        );
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
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
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
}
