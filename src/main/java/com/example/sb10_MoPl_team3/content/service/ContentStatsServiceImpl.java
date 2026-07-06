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
import org.springframework.dao.DataAccessException;
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
    ContentStats stats = contentStatsRepository.findByIdForUpdate(contentId).orElse(null);
    if (stats == null) {
      // 서버 인스턴스가 여러 대라 같은 콘텐츠의 통계를 동시에 처음 만들려고 할 수 있다.
      // 생성은 별도 트랜잭션(REQUIRES_NEW)에서 시도하므로, 다른 인스턴스가 먼저 만들었거나
      // 만드는 중이라 PK 충돌/잠금 대기 타임아웃이 나도 그 트랜잭션만 롤백될 뿐 지금 이
      // 트랜잭션(과 커넥션)은 영향받지 않는다. 그 뒤 다시 잠금 조회를 하면 누가 만들었든
      // 그 행을 안전하게 이어서 쓸 수 있다.
      try {
        contentStatsRepository.createDefaultIgnoringConflict(contentId);
      } catch (DataAccessException e) {
        // 다른 인스턴스/스레드가 먼저 만들었거나 만드는 중인 경우: 무시하고 아래에서 다시 조회한다.
      }
      stats = contentStatsRepository.findByIdForUpdate(contentId)
          .orElseThrow(() -> new IllegalStateException(
              "ContentStats 생성에 실패했습니다. contentId=" + contentId));
    }

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
    // 리뷰가 이미 쌓여있던 콘텐츠일 수 있으므로 빈 통계로 채우지 않고,
    // 콘텐츠마다 실제 리뷰를 집계해서 정확한 값으로 채운다.
    List<Content> contentsWithoutStats = contentRepository.findAllWithoutStats();
    for (Content content : contentsWithoutStats) {
      recalculate(content.getId());
    }
  }
}
