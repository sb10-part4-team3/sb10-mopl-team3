package com.example.sb10_MoPl_team3.content.mapper;

import com.example.sb10_MoPl_team3.content.dto.ContentDto;
import com.example.sb10_MoPl_team3.content.dto.ContentSummary;
import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.entity.ContentStats;
import com.example.sb10_MoPl_team3.global.file.FileStorageService;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContentMapper {

  private final FileStorageService fileStorageService;

  public ContentDto toDto(Content content, ContentStats stats, List<String> tags) {
    BigDecimal averageRating = stats != null ? stats.getAverageRating() : BigDecimal.ZERO;
    int reviewCount = stats != null ? stats.getReviewCount() : 0;
    int viewerCount = stats != null ? stats.getViewerCount() : 0;

    return new ContentDto(
            content.getId(),
            content.getType(),
            content.getTitle(),
            content.getDescription(),
            fileStorageService.toAccessibleUrl(content.getThumbnailUrl()),
            tags,
            averageRating.doubleValue(),
            reviewCount,
            (long) viewerCount
    );
  }

  public ContentSummary toSummary(Content content, ContentStats stats, List<String> tags) {
    BigDecimal averageRating = stats != null ? stats.getAverageRating() : BigDecimal.ZERO;
    int reviewCount = stats != null ? stats.getReviewCount() : 0;

    return new ContentSummary(
            content.getId(),
            content.getType(),
            content.getTitle(),
            content.getDescription(),
            fileStorageService.toAccessibleUrl(content.getThumbnailUrl()),
            tags,
            averageRating.doubleValue(),
            reviewCount
    );
  }
}