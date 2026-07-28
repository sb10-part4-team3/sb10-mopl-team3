package com.example.sb10_MoPl_team3.notification.service;

import com.example.sb10_MoPl_team3.follow.repository.FollowRepository;
import com.example.sb10_MoPl_team3.notification.entity.NotificationFanoutJob;
import com.example.sb10_MoPl_team3.notification.event.NotificationFanoutEvent;
import com.example.sb10_MoPl_team3.notification.kafka.NotificationFanoutKafkaMessage;
import com.example.sb10_MoPl_team3.playlist.repository.PlaylistSubscriptionRepository;
import java.time.Clock;
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
public class NotificationFanoutJobService {

    private static final int BATCH_SIZE = 100;

    private final FollowRepository followRepository;
    private final PlaylistSubscriptionRepository playlistSubscriptionRepository;
    private final NotificationFanoutBatchService batchService;
    private final NotificationFanoutJobStatusService statusService;
    private final Clock clock;

    public void process(NotificationFanoutKafkaMessage message) {
        NotificationFanoutJob job = statusService.start(message);
        if (job.isCompleted()) {
            log.info("이미 완료된 알림 팬아웃 job을 건너뜁니다. outboxId={}, jobId={}",
                    message.outboxId(), job.getId());
            return;
        }

        NotificationFanoutEvent event = job.toEvent();
        int page = job.getNextPage();
        long startedAt = System.nanoTime();
        try {
            Slice<UUID> receiverIds;
            do {
                receiverIds = findReceiverIds(event, PageRequest.of(page, BATCH_SIZE));
                int processedCount = 0;
                if (receiverIds.hasContent()) {
                    processedCount = batchService.saveBatch(
                            receiverIds.getContent(),
                            event,
                            job.getOutboxId());
                }
                statusService.markPageProcessed(job.getId(), page, processedCount);
                page++;
            } while (receiverIds.hasNext());

            statusService.markCompleted(job.getId(), clock.instant());
            long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
            log.info("알림 팬아웃 job 처리 완료: outboxId={}, jobId={}, elapsedMs={}",
                    message.outboxId(), job.getId(), elapsedMillis);
        } catch (RuntimeException exception) {
            statusService.markFailed(job.getId(), exception.getMessage());
            log.error("알림 팬아웃 job 처리 실패: outboxId={}, jobId={}, nextPage={}",
                    message.outboxId(), job.getId(), page, exception);
            throw exception;
        }
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
