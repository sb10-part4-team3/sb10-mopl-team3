package com.example.sb10_MoPl_team3.content.event;

import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import com.example.sb10_MoPl_team3.content.service.ContentStatsService;
import com.example.sb10_MoPl_team3.review.event.ReviewStatsChangedEvent;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentStatsReviewEventListenerTest {

  @Mock
  private ContentStatsService contentStatsService;

  private ContentStatsReviewEventListener listener;

  @BeforeEach
  void setUp() {
    listener = new ContentStatsReviewEventListener(contentStatsService);
  }

  @Test
  @DisplayName("이벤트를 받으면 해당 콘텐츠의 통계를 재계산한다")
  void onReviewStatsChanged_recalculatesStats() {
    UUID contentId = UUID.randomUUID();

    listener.onReviewStatsChanged(new ReviewStatsChangedEvent(contentId));

    then(contentStatsService).should().recalculate(contentId);
  }

  @Test
  @DisplayName("재계산 중 예외가 발생해도 리스너 밖으로 전파하지 않는다")
  void onReviewStatsChanged_swallowsException() {
    UUID contentId = UUID.randomUUID();
    willThrow(new IllegalStateException("boom"))
        .given(contentStatsService).recalculate(contentId);

    listener.onReviewStatsChanged(new ReviewStatsChangedEvent(contentId));

    then(contentStatsService).should().recalculate(contentId);
  }
}
