package com.example.sb10_MoPl_team3.content.dto;

import com.example.sb10_MoPl_team3.content.ContentType;
import java.util.List;
import java.util.UUID;

public record ContentDto(
    UUID id,
    ContentType type,
    String title,
    String description,
    String thumbnailUrl,
    List<String> tags,
    Double averageRating,
    int reviewCount,
    long watcherCount
) {

}
