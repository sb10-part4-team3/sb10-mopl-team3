package com.example.sb10_MoPl_team3.notification.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;

@ExtendWith(MockitoExtension.class)
class NotificationFanoutServiceTest {

    @Mock FollowRepository followRepository;
    @Mock PlaylistSubscriptionRepository playlistSubscriptionRepository;
    @Mock NotificationFanoutBatchService batchService;
    @InjectMocks NotificationFanoutService fanoutService;

    @Test
    @DisplayName("팔로워를 페이지로 조회해 알림을 일괄 저장한다")
    void handle_savesFollowerNotificationsInBatch() {
        UUID sourceId = UUID.randomUUID();
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        given(followRepository.findFollowerIdsByFolloweeId(
                org.mockito.ArgumentMatchers.eq(sourceId), any(Pageable.class)))
                .willReturn(new SliceImpl<>(List.of(firstId, secondId)));
        given(batchService.saveBatch(any(), any())).willReturn(2);

        fanoutService.handle(new NotificationFanoutEvent(
                NotificationAudienceType.FOLLOWERS,
                sourceId,
                "팔로우 활동",
                "새로운 활동입니다.",
                NotificationLevel.INFO
        ));

        then(batchService).should().saveBatch(
                org.mockito.ArgumentMatchers.eq(List.of(firstId, secondId)), any());
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
