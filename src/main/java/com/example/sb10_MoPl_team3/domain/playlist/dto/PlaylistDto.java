package com.example.sb10_MoPl_team3.domain.playlist.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlaylistDto(
        UUID id,
        // 후에 UserSummery로 변경 필요
        Object owner,
        String title,
        String description,
        Instant updatedAt,
        Long subscriberCount,
        boolean subscribedByMe,
        List<Object> contents
) {
}
