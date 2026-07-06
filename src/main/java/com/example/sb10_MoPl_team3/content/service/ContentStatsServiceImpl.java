package com.example.sb10_MoPl_team3.content.service;

import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.entity.ContentStats;
import com.example.sb10_MoPl_team3.content.repository.ContentRepository;
import com.example.sb10_MoPl_team3.content.repository.ContentStatsRepository;
import com.example.sb10_MoPl_team3.review.enums.ReviewStatus;
import com.example.sb10_MoPl_team3.review.repository.ReviewRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
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
    // 같은 콘텐츠에 대한 recalculate()가 동시에 들어와도 한쪽이 잠금을 잡고 있는 동안
    // 다른 쪽은 대기하게 만들어, 리뷰 집계를 항상 최신 커밋 상태 기준으로 계산하게 한다.
    // (잠금 없이 먼저 집계부터 읽으면, 늦게 커밋되는 트랜잭션이 오래된 값으로
    //  방금 반영된 최신 값을 덮어쓰는 lost update가 발생할 수 있다.)
    ContentStats stats = contentStatsRepository.findByIdForUpdate(contentId)
        .orElseGet(() -> {
          Content content = contentRepository.getReferenceById(contentId);
          return contentStatsRepository.save(ContentStats.createDefault(content));
        });

    long reviewCount = reviewRepository.countByContent_IdAndStatus(contentId, ReviewStatus.ACTIVE);
    Double average = reviewRepository.findAverageRatingByContentIdAndStatus(contentId,
        ReviewStatus.ACTIVE);
    BigDecimal averageRating = average != null
        ? BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP)
        : BigDecimal.ZERO;

    stats.updateStats(averageRating, (int) reviewCount);
  }

  @Override
  @Transactional
  public void backfillMissingStats() {
    List<Content> contentsWithoutStats = contentRepository.findAllWithoutStats();
    if (contentsWithoutStats.isEmpty()) {
      return;
    }

    List<ContentStats> defaults = contentsWithoutStats.stream()
        .map(ContentStats::createDefault)
        .toList();
    contentStatsRepository.saveAll(defaults);
  }
}
