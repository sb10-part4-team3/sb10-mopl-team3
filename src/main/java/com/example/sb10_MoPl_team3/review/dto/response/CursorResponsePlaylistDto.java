package com.example.sb10_MoPl_team3.review.dto.response;

import java.util.List;
import java.util.UUID;

public record CursorResponsePlaylistDto<T>(
        List<T> data,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext,
        Long totalCount,
        String sortBy,
        String sortDirection
) {
}
