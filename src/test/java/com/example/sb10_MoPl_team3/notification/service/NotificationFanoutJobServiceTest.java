package com.example.sb10_MoPl_team3.notification.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.sb10_MoPl_team3.follow.repository.FollowRepository;
import com.example.sb10_MoPl_team3.notification.entity.NotificationFanoutJob;
import com.example.sb10_MoPl_team3.notification.enums.NotificationLevel;
import com.example.sb10_MoPl_team3.notification.event.NotificationAudienceType;
import com.example.sb10_MoPl_team3.notification.kafka.NotificationFanoutKafkaMessage;
import com.example.sb10_MoPl_team3.playlist.repository.PlaylistSubscriptionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationFanoutJobServiceTest {

    @Mock
    FollowRepository followRepository;

    @Mock
    PlaylistSubscriptionRepository playlistSubscriptionRepository;

    @Mock
    NotificationFanoutBatchService batchService;

    @Mock
    NotificationFanoutJobStatusService statusService;

    Clock clock = Clock.fixed(Instant.parse("2026-07-09T00:00:00Z"), ZoneOffset.UTC);

    NotificationFanoutJobService service;

    @BeforeEach
    void setUp() {
        service = new NotificationFanoutJobService(
                followRepository,
                playlistSubscriptionRepository,
                batchService,
                statusService,
                clock
        );
    }

    @Test
    @DisplayName("여러 페이지를 처리하며 페이지마다 진행 offset을 저장하고 완료 처리한다")
    void process_updatesPageOffsetAndCompletes() {
        NotificationFanoutKafkaMessage message = message();
        NotificationFanoutJob job = job(message);
        UUID firstReceiverId = UUID.randomUUID();
        UUID secondReceiverId = UUID.randomUUID();
        given(statusService.start(message)).willReturn(job);
        given(followRepository.findFollowerIdsByFolloweeId(
                message.sourceId(), PageRequest.of(0, 100)))
                .willReturn(new SliceImpl<>(
                        List.of(firstReceiverId), PageRequest.of(0, 100), true));
        given(followRepository.findFollowerIdsByFolloweeId(
                message.sourceId(), PageRequest.of(1, 100)))
                .willReturn(new SliceImpl<>(
                        List.of(secondReceiverId), PageRequest.of(1, 100), false));
        given(batchService.saveBatch(eq(List.of(firstReceiverId)), any(), eq(message.outboxId())))
                .willReturn(1);
        given(batchService.saveBatch(eq(List.of(secondReceiverId)), any(), eq(message.outboxId())))
                .willReturn(1);

        service.process(message);

        then(statusService).should().markPageProcessed(job.getId(), 0, 1);
        then(statusService).should().markPageProcessed(job.getId(), 1, 1);
        then(statusService).should().markCompleted(job.getId(), clock.instant());
        then(statusService).should(never()).markFailed(any(), any());
    }

    @Test
    @DisplayName("중간 페이지 실패 시 마지막 성공 페이지까지만 offset을 저장하고 실패 상태를 기록한다")
    void process_marksFailedWithLastSuccessfulOffset() {
        NotificationFanoutKafkaMessage message = message();
        NotificationFanoutJob job = job(message);
        UUID firstReceiverId = UUID.randomUUID();
        UUID secondReceiverId = UUID.randomUUID();
        given(statusService.start(message)).willReturn(job);
        given(followRepository.findFollowerIdsByFolloweeId(
                message.sourceId(), PageRequest.of(0, 100)))
                .willReturn(new SliceImpl<>(
                        List.of(firstReceiverId), PageRequest.of(0, 100), true));
        given(followRepository.findFollowerIdsByFolloweeId(
                message.sourceId(), PageRequest.of(1, 100)))
                .willReturn(new SliceImpl<>(
                        List.of(secondReceiverId), PageRequest.of(1, 100), false));
        given(batchService.saveBatch(eq(List.of(firstReceiverId)), any(), eq(message.outboxId())))
                .willReturn(1);
        RuntimeException exception = new RuntimeException("batch failed");
        given(batchService.saveBatch(eq(List.of(secondReceiverId)), any(), eq(message.outboxId())))
                .willThrow(exception);

        assertThatThrownBy(() -> service.process(message))
                .isSameAs(exception);

        then(statusService).should().markPageProcessed(job.getId(), 0, 1);
        then(statusService).should(never()).markPageProcessed(eq(job.getId()), eq(1), any(Integer.class));
        then(statusService).should().markFailed(job.getId(), "batch failed");
        then(statusService).should(never()).markCompleted(any(), any());
    }

    @Test
    @DisplayName("실패했던 job은 저장된 nextPage부터 재개한다")
    void process_resumesFromNextPage() {
        NotificationFanoutKafkaMessage message = message();
        NotificationFanoutJob job = job(message);
        job.markPageProcessed(0, 1);
        UUID receiverId = UUID.randomUUID();
        given(statusService.start(message)).willReturn(job);
        given(followRepository.findFollowerIdsByFolloweeId(
                message.sourceId(), PageRequest.of(1, 100)))
                .willReturn(new SliceImpl<>(
                        List.of(receiverId), PageRequest.of(1, 100), false));
        given(batchService.saveBatch(eq(List.of(receiverId)), any(), eq(message.outboxId())))
                .willReturn(1);

        service.process(message);

        then(followRepository).should(never()).findFollowerIdsByFolloweeId(
                message.sourceId(), PageRequest.of(0, 100));
        then(statusService).should().markPageProcessed(job.getId(), 1, 1);
        then(statusService).should().markCompleted(job.getId(), clock.instant());
    }

    private NotificationFanoutKafkaMessage message() {
        return new NotificationFanoutKafkaMessage(
                UUID.randomUUID(),
                NotificationAudienceType.FOLLOWERS,
                UUID.randomUUID(),
                "시청 시작",
                "새로운 활동입니다.",
                NotificationLevel.INFO
        );
    }

    private NotificationFanoutJob job(NotificationFanoutKafkaMessage message) {
        NotificationFanoutJob job = new NotificationFanoutJob(message);
        ReflectionTestUtils.setField(job, "id", UUID.randomUUID());
        return job;
    }
}
