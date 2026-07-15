package com.example.sb10_MoPl_team3.watchingsession.service;

import com.example.sb10_MoPl_team3.content.repository.ContentStatsRepository;
import com.example.sb10_MoPl_team3.watchingsession.repository.WatchingSessionRedisRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class WatchingSessionViewerCountServiceTest {

    @Mock WatchingSessionRedisRepository redisRepository;
    @Mock ContentStatsRepository contentStatsRepository;
    @InjectMocks WatchingSessionViewerCountService service;

    @Test
    void sync_updatesViewerCountFromRedisWatcherCount() {
        UUID contentId = UUID.randomUUID();
        given(redisRepository.countWatchers(contentId)).willReturn(3L);
        given(contentStatsRepository.updateViewerCount(eq(contentId), eq(3), any(Instant.class)))
                .willReturn(1);

        service.sync(contentId);

        then(contentStatsRepository).should()
                .updateViewerCount(eq(contentId), eq(3), any(Instant.class));
    }
}
