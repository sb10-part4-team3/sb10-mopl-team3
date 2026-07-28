package com.example.sb10_MoPl_team3.watchingsession.service;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.dto.ContentSummary;
import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;
import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionChange;
import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionChangeType;
import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionDto;
import com.example.sb10_MoPl_team3.watchingsession.repository.WatchingSessionRedisRepository;
import com.example.sb10_MoPl_team3.watchingsession.websocket.WatchingSessionBroadcastPublisher;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class WatchingSessionStalePresenceReaperTest {

    @Mock WatchingSessionRedisRepository redisRepository;
    @Mock WatchingSessionPresenceService presenceService;
    @Mock WatchingSessionBroadcastPublisher broadcastPublisher;
    @InjectMocks WatchingSessionStalePresenceReaper reaper;

    @Test
    void reap_removesStaleWatchersAndBroadcastsChanges() {
        UUID contentId = UUID.randomUUID();
        UUID watcherId = UUID.randomUUID();
        WatchingSessionChange change = change(contentId, watcherId);
        doAnswer(invocation -> {
            Consumer<UUID> consumer = invocation.getArgument(0);
            consumer.accept(contentId);
            return null;
        }).when(redisRepository).forEachActiveContentId(org.mockito.ArgumentMatchers.any());
        given(presenceService.removeStaleWatchers(contentId)).willReturn(List.of(change));

        reaper.reap();

        then(redisRepository).should().forEachActiveContentId(org.mockito.ArgumentMatchers.any());
        then(presenceService).should().removeStaleWatchers(contentId);
        then(broadcastPublisher).should().publish(change);
    }

    @Test
    void reap_continuesWhenContentCleanupFails() {
        UUID contentId = UUID.randomUUID();
        doAnswer(invocation -> {
            Consumer<UUID> consumer = invocation.getArgument(0);
            consumer.accept(contentId);
            return null;
        }).when(redisRepository).forEachActiveContentId(org.mockito.ArgumentMatchers.any());
        given(presenceService.removeStaleWatchers(contentId))
                .willThrow(new RuntimeException("boom"));

        reaper.reap();

        then(broadcastPublisher).should(never())
                .publish(org.mockito.ArgumentMatchers.any());
    }

    private WatchingSessionChange change(UUID contentId, UUID watcherId) {
        return new WatchingSessionChange(
                WatchingSessionChangeType.LEAVE,
                new WatchingSessionDto(
                        UUID.randomUUID(),
                        Instant.now(),
                        new UserSummary(watcherId, "시청자", null),
                        new ContentSummary(
                                contentId,
                                ContentType.MOVIE,
                                "콘텐츠",
                                "설명",
                                "thumbnail",
                                List.of(),
                                0.0,
                                0
                        )
                ),
                0
        );
    }
}
