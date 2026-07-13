package com.example.sb10_MoPl_team3.content.event;

import com.example.sb10_MoPl_team3.content.service.ContentStatsService;
import com.example.sb10_MoPl_team3.review.event.ReviewStatsChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentStatsReviewEventListener {

  private final ContentStatsService contentStatsService;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onReviewStatsChanged(ReviewStatsChangedEvent event) {
    try {
      contentStatsService.recalculate(event.contentId());
    } catch (RuntimeException exception) {
      log.warn("리뷰 통계 재계산 실패: contentId={}", event.contentId(), exception);
    }
  }
}