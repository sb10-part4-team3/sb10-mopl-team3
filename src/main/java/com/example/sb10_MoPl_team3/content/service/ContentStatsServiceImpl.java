package com.example.sb10_MoPl_team3.content.service;

import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.entity.ContentStats;
import com.example.sb10_MoPl_team3.content.repository.ContentRepository;
import com.example.sb10_MoPl_team3.content.repository.ContentStatsRepository;
import com.example.sb10_MoPl_team3.review.enums.ReviewStatus;
import com.example.sb10_MoPl_team3.review.repository.ReviewRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentStatsServiceImpl implements ContentStatsService {

  private final ContentRepository contentRepository;
  private final ContentStatsRepository contentStatsRepository;
  private final ReviewRepository reviewRepository;

  @Override
  @Transactional
  public void recalculate(UUID contentId) {
    long reviewCount = reviewRepository.countByContent_IdAndStatus(contentId, ReviewStatus.ACTIVE);
    Double average = reviewRepository.findAverageRatingByContentIdAndStatus(contentId,
        ReviewStatus.ACTIVE);
    BigDecimal averageRating = average != null
        ? BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP)
        : BigDecimal.ZERO;

    ContentStats stats = contentStatsRepository.findById(contentId)
        .orElseGet(() -> {
          Content content = contentRepository.getReferenceById(contentId);
          return contentStatsRepository.save(ContentStats.builder()
              .content(content)
              .averageRating(BigDecimal.ZERO)
              .reviewCount(0)
              .viewerCount(0)
              .build());
        });

    stats.updateStats(averageRating, (int) reviewCount);
  }
}
