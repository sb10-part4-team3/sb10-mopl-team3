package com.example.sb10_MoPl_team3.review.dto.request;

import java.util.UUID;

public record ReviewFindAllRequest(
        UUID contentId,
        String cursor,
        UUID idAfter,
        int limit,
        String sortDirection,
        String sortBy
) {
}
