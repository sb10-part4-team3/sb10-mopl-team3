package com.example.sb10_MoPl_team3.watchingsession.dto;

import java.util.UUID;

public record WatchingSessionFindAllRequest(
        UUID contentId, String watcherNameLike, String cursor, UUID idAfter,
        int limit, String sortDirection, String sortBy
) {
}
