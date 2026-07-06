package com.example.sb10_MoPl_team3.notification.service;

import com.example.sb10_MoPl_team3.follow.repository.FollowRepository;
import com.example.sb10_MoPl_team3.notification.event.NotificationFanoutEvent;
import com.example.sb10_MoPl_team3.playlist.repository.PlaylistSubscriptionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationFanoutService {

    private static final int BATCH_SIZE = 100;

    private final FollowRepository followRepository;
    private final PlaylistSubscriptionRepository playlistSubscriptionRepository;
    private final NotificationFanoutBatchService batchService;

    public void handle(NotificationFanoutEvent event) {
        long startedAt = System.nanoTime();
        int processedCount = 0;
        int page = 0;
        Slice<UUID> receiverIds;
        do {
            receiverIds = findReceiverIds(event, PageRequest.of(page, BATCH_SIZE));
            if (receiverIds.hasContent()) {
                processedCount += batchService.saveBatch(receiverIds.getContent(), event);
            }
            page++;
        } while (receiverIds.hasNext());

        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
        log.info("알림 팬아웃 처리 완료: audienceType={}, sourceId={}, receiverCount={}, "
                        + "batchCount={}, elapsedMs={}",
                event.audienceType(), event.sourceId(), processedCount, page, elapsedMillis);
    }

    private Slice<UUID> findReceiverIds(NotificationFanoutEvent event, Pageable pageable) {
        return switch (event.audienceType()) {
            case FOLLOWERS -> followRepository.findFollowerIdsByFolloweeId(
                    event.sourceId(), pageable);
            case PLAYLIST_SUBSCRIBERS ->
                    playlistSubscriptionRepository.findSubscriberUserIdsByPlaylistId(
                            event.sourceId(), pageable);
        };
    }
}
