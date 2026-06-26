package com.example.sb10_MoPl_team3.content.mapper;

import com.example.sb10_MoPl_team3.content.dto.ContentDto;
import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.entity.ContentStats;
import java.math.BigDecimal;
import java.util.List;

public class ContentMapper {

  private ContentMapper() {
  }

  public static ContentDto toDto(Content content, ContentStats stats, List<String> tags) {
    BigDecimal averageRating = stats != null ? stats.getAverageRating() : BigDecimal.ZERO;
    int reviewCount = stats != null ? stats.getReviewCount() : 0;
    int viewerCount = stats != null ? stats.getViewerCount() : 0;

    return new ContentDto(
        content.getId(),
        content.getType(),
        content.getTitle(),
        content.getDescription(),
        content.getThumbnailUrl(),
        tags,
        averageRating.doubleValue(),
        reviewCount,
        (long) viewerCount
    );
  }
}