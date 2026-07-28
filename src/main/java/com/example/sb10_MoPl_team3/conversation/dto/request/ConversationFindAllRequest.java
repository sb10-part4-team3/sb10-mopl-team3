package com.example.sb10_MoPl_team3.conversation.dto.request;

import java.util.UUID;

public record ConversationFindAllRequest(
    String keywordLike,
    String cursor,
    UUID idAfter,
    int limit,
    String sortDirection,
    String sortBy
) {
}
