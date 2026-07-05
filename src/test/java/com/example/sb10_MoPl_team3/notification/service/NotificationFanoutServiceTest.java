package com.example.sb10_MoPl_team3.notification.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.example.sb10_MoPl_team3.follow.repository.FollowRepository;
import com.example.sb10_MoPl_team3.notification.enums.NotificationLevel;
import com.example.sb10_MoPl_team3.notification.event.NotificationAudienceType;
import com.example.sb10_MoPl_team3.notification.event.NotificationFanoutEvent;
import com.example.sb10_MoPl_team3.playlist.repository.PlaylistSubscriptionRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;

@ExtendWith(MockitoExtension.class)
class NotificationFanoutServiceTest {

    @Mock FollowRepository followRepository;
    @Mock PlaylistSubscriptionRepository playlistSubscriptionRepository;
    @Mock NotificationFanoutBatchService batchService;
    @InjectMocks NotificationFanoutService fanoutService;

    @Test
    @DisplayName("팔로워가 여러 페이지면 모든 페이지의 알림을 배치 저장한다")
    void handle_savesAllFollowerPagesInBatch() {
        UUID sourceId = UUID.randomUUID();
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        Pageable firstPage = PageRequest.of(0, 100);
        Pageable secondPage = PageRequest.of(1, 100);
        given(followRepository.findFollowerIdsByFolloweeId(sourceId, firstPage))
                .willReturn(new SliceImpl<>(List.of(firstId), firstPage, true));
        given(followRepository.findFollowerIdsByFolloweeId(sourceId, secondPage))
                .willReturn(new SliceImpl<>(List.of(secondId), secondPage, false));
        given(batchService.saveBatch(any(), any())).willReturn(1);

        fanoutService.handle(new NotificationFanoutEvent(
                NotificationAudienceType.FOLLOWERS,
                sourceId,
                "팔로우 활동",
                "새로운 활동입니다.",
                NotificationLevel.INFO
        ));

        then(followRepository).should().findFollowerIdsByFolloweeId(sourceId, firstPage);
        then(followRepository).should().findFollowerIdsByFolloweeId(sourceId, secondPage);
        then(batchService).should().saveBatch(eq(List.of(firstId)), any());
        then(batchService).should().saveBatch(eq(List.of(secondId)), any());
        then(batchService).should(times(2)).saveBatch(any(), any());
    }

    @Test
    @DisplayName("플레이리스트 구독자를 조회해 알림을 배치 저장한다")
    void handle_savesPlaylistSubscriberNotificationsInBatch() {
        UUID playlistId = UUID.randomUUID();
        UUID subscriberId = UUID.randomUUID();
        given(playlistSubscriptionRepository.findSubscriberUserIdsByPlaylistId(
                eq(playlistId), any(Pageable.class)))
                .willReturn(new SliceImpl<>(List.of(subscriberId)));
        given(batchService.saveBatch(any(), any())).willReturn(1);

        fanoutService.handle(new NotificationFanoutEvent(
                NotificationAudienceType.PLAYLIST_SUBSCRIBERS,
                playlistId,
                "플레이리스트 업데이트",
                "콘텐츠가 추가되었습니다.",
                NotificationLevel.INFO
        ));

        then(batchService).should().saveBatch(eq(List.of(subscriberId)), any());
    }

    @Test
    @DisplayName("플레이리스트 구독자가 없으면 알림을 저장하지 않는다")
    void handle_skipsEmptyPlaylistSubscribers() {
        UUID playlistId = UUID.randomUUID();
        given(playlistSubscriptionRepository.findSubscriberUserIdsByPlaylistId(
                org.mockito.ArgumentMatchers.eq(playlistId), any(Pageable.class)))
                .willReturn(new SliceImpl<>(List.of()));

        fanoutService.handle(new NotificationFanoutEvent(
                NotificationAudienceType.PLAYLIST_SUBSCRIBERS,
                playlistId,
                "플레이리스트 업데이트",
                "콘텐츠가 추가되었습니다.",
                NotificationLevel.INFO
        ));

        then(batchService).shouldHaveNoInteractions();
    }
}
