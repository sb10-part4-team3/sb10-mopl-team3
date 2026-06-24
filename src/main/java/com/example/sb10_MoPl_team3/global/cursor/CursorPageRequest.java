package com.example.sb10_MoPl_team3.global.cursor;

import java.util.UUID;

public record CursorPageRequest(
    String cursor,
    UUID idAfter,
    int size,
    String sortBy,
    String sortDirection
) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;
    public static final String DEFAULT_SORT_DIRECTION = "DESC";

    public CursorPageRequest {
        if (size <= 0) {
            size = DEFAULT_SIZE;
        } else if (size > MAX_SIZE) {
            size = MAX_SIZE;
        }
        if (sortDirection == null || sortDirection.isBlank()) {
            sortDirection = DEFAULT_SORT_DIRECTION;
        }
    }

    public boolean hasCursor() {
        return cursor != null && !cursor.isBlank();
    }

    public boolean isAscending() {
        return "ASC".equalsIgnoreCase(sortDirection);
    }
}
