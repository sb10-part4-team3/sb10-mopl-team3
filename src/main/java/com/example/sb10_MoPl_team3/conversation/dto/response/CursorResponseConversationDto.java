package com.example.sb10_MoPl_team3.conversation.dto.response;

import java.util.List;
import java.util.UUID;

public record CursorResponseConversationDto<T>(
    List<T> data,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    Long totalCount,
    String sortBy,
    String sortDirection
) {
}
