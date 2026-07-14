package com.example.sb10_MoPl_team3.content.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.sb10_MoPl_team3.content.repository.ContentStatsRepository;
import com.example.sb10_MoPl_team3.watchingsession.event.WatchingSessionJoinedEvent;
import com.example.sb10_MoPl_team3.watchingsession.event.WatchingSessionLeftEvent;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentStatsWatchingSessionEventListenerTest {

  @Mock
  private ContentStatsRepository contentStatsRepository;

  private ContentStatsWatchingSessionEventListener listener;

  @BeforeEach
  void setUp() {
    listener = new ContentStatsWatchingSessionEventListener(contentStatsRepository);
  }

  @Test
  @DisplayName("시청 시작 이벤트를 받으면 viewerCount를 증가시킨다")
  void onJoined_incrementsViewerCount() {
    UUID contentId = UUID.randomUUID();
    given(contentStatsRepository.incrementViewerCount(any(UUID.class), any(Instant.class)))
        .willReturn(1);

    listener.onJoined(new WatchingSessionJoinedEvent(contentId));

    then(contentStatsRepository).should().incrementViewerCount(any(UUID.class), any(Instant.class));
  }

  @Test
  @DisplayName("증가 대상 콘텐츠 통계가 없어도 예외를 리스너 밖으로 전파하지 않는다")
  void onJoined_swallowsExceptionWhenStatsMissing() {
    UUID contentId = UUID.randomUUID();
    given(contentStatsRepository.incrementViewerCount(any(UUID.class), any(Instant.class)))
        .willReturn(0);

    listener.onJoined(new WatchingSessionJoinedEvent(contentId));

    then(contentStatsRepository).should().incrementViewerCount(any(UUID.class), any(Instant.class));
  }

  @Test
  @DisplayName("증가 중 런타임 예외가 발생해도 리스너 밖으로 전파하지 않는다")
  void onJoined_swallowsRuntimeException() {
    UUID contentId = UUID.randomUUID();
    given(contentStatsRepository.incrementViewerCount(any(UUID.class), any(Instant.class)))
        .willThrow(new RuntimeException("boom"));

    listener.onJoined(new WatchingSessionJoinedEvent(contentId));

    then(contentStatsRepository).should().incrementViewerCount(any(UUID.class), any(Instant.class));
  }

  @Test
  @DisplayName("시청 종료 이벤트를 받으면 viewerCount를 감소시킨다")
  void onLeft_decrementsViewerCount() {
    UUID contentId = UUID.randomUUID();
    given(contentStatsRepository.decrementViewerCount(any(UUID.class), any(Instant.class)))
        .willReturn(1);

    listener.onLeft(new WatchingSessionLeftEvent(contentId));

    then(contentStatsRepository).should().decrementViewerCount(any(UUID.class), any(Instant.class));
    then(contentStatsRepository).should(org.mockito.Mockito.never()).existsById(any(UUID.class));
  }

  @Test
  @DisplayName("viewerCount가 이미 0이어도(통계는 존재) 예외 없이 넘어간다")
  void onLeft_alreadyZero_doesNotThrow() {
    UUID contentId = UUID.randomUUID();
    given(contentStatsRepository.decrementViewerCount(any(UUID.class), any(Instant.class)))
        .willReturn(0);
    given(contentStatsRepository.existsById(contentId)).willReturn(true);

    listener.onLeft(new WatchingSessionLeftEvent(contentId));

    then(contentStatsRepository).should().existsById(contentId);
  }

  @Test
  @DisplayName("감소 대상 콘텐츠 통계 자체가 없어도 예외를 리스너 밖으로 전파하지 않는다")
  void onLeft_swallowsExceptionWhenStatsMissing() {
    UUID contentId = UUID.randomUUID();
    given(contentStatsRepository.decrementViewerCount(any(UUID.class), any(Instant.class)))
        .willReturn(0);
    given(contentStatsRepository.existsById(contentId)).willReturn(false);

    listener.onLeft(new WatchingSessionLeftEvent(contentId));

    then(contentStatsRepository).should().existsById(contentId);
  }

  @Test
  @DisplayName("감소 중 런타임 예외가 발생해도 리스너 밖으로 전파하지 않는다")
  void onLeft_swallowsRuntimeException() {
    UUID contentId = UUID.randomUUID();
    given(contentStatsRepository.decrementViewerCount(any(UUID.class), any(Instant.class)))
        .willThrow(new RuntimeException("boom"));

    listener.onLeft(new WatchingSessionLeftEvent(contentId));

    then(contentStatsRepository).should().decrementViewerCount(any(UUID.class), any(Instant.class));
  }
}
