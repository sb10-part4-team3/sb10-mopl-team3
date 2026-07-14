package com.example.sb10_MoPl_team3.content.event;

import com.example.sb10_MoPl_team3.content.repository.ContentStatsRepository;
import com.example.sb10_MoPl_team3.watchingsession.event.WatchingSessionJoinedEvent;
import com.example.sb10_MoPl_team3.watchingsession.event.WatchingSessionLeftEvent;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@RequiredArgsConstructor
@Component
@Slf4j
public class ContentStatsWatchingSessionEventListener {

  private final ContentStatsRepository contentStatsRepository;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onJoined(WatchingSessionJoinedEvent event) {
    try {
      if (contentStatsRepository.incrementViewerCount(event.contentId(), Instant.now()) != 1) {
        throw new IllegalStateException("콘텐츠 통계가 존재하지 않습니다. contentId=" + event.contentId());
      }
    } catch (RuntimeException exception) {
      log.warn("시청자 수 증가 실패: contentId={}", event.contentId(), exception);
    }
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onLeft(WatchingSessionLeftEvent event) {
    try {
      if (contentStatsRepository.decrementViewerCount(event.contentId(), Instant.now()) == 1) {
        return;
      }
      if (!contentStatsRepository.existsById(event.contentId())) {
        throw new IllegalStateException("콘텐츠 통계가 존재하지 않습니다. contentId=" + event.contentId());
      }
      log.warn("시청 세션은 존재하지만 viewerCount가 이미 0입니다. contentId={}", event.contentId());
    } catch (RuntimeException exception) {
      log.warn("시청자 수 감소 실패: contentId={}", event.contentId(), exception);
    }
  }
}
