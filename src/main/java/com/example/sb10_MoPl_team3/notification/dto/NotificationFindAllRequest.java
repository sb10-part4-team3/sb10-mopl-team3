package com.example.sb10_MoPl_team3.notification.dto;

import java.util.UUID;

public record NotificationFindAllRequest(
        String cursor,
        UUID idAfter,
        int limit,
        String sortDirection,
        String sortBy
) {
}
