package com.example.sb10_MoPl_team3.playlist.dto.request;

import java.util.UUID;

public record PlaylistFindAllRequest(
        String keywordLike,
        UUID ownerIdEqual,
        UUID subscriberIdEqual,
        String cursor,
        UUID idAfter,
        Integer limit,
        String sortDirection,
        String sortBy
) {
}
