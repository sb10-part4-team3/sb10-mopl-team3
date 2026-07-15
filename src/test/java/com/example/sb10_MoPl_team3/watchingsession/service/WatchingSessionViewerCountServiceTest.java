package com.example.sb10_MoPl_team3.watchingsession.service;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.entity.ContentStats;
import com.example.sb10_MoPl_team3.content.repository.ContentStatsRepository;
import com.example.sb10_MoPl_team3.watchingsession.repository.WatchingSessionRedisRepository;
import java.time.Instant;
import java.util.Optional;
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
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class WatchingSessionViewerCountServiceTest {

    @Mock WatchingSessionRedisRepository redisRepository;
    @Mock ContentStatsRepository contentStatsRepository;
    @InjectMocks WatchingSessionViewerCountService service;

    @Test
    void sync_updatesViewerCountFromRedisWatcherCount() {
        UUID contentId = UUID.randomUUID();
        given(redisRepository.countWatchers(contentId)).willReturn(3L);
        given(contentStatsRepository.findById(contentId)).willReturn(Optional.of(stats(1)));
        given(contentStatsRepository.updateViewerCount(eq(contentId), eq(3), any(Instant.class)))
                .willReturn(1);

        service.sync(contentId);

        then(contentStatsRepository).should()
                .updateViewerCount(eq(contentId), eq(3), any(Instant.class));
    }

    @Test
    void sync_skipsUpdateWhenViewerCountAlreadyMatches() {
        UUID contentId = UUID.randomUUID();
        given(redisRepository.countWatchers(contentId)).willReturn(3L);
        given(contentStatsRepository.findById(contentId)).willReturn(Optional.of(stats(3)));

        service.sync(contentId);

        then(contentStatsRepository).should(never())
                .updateViewerCount(eq(contentId), eq(3), any(Instant.class));
    }

    @Test
    void sync_skipsUpdateWhenStatsMissing() {
        UUID contentId = UUID.randomUUID();
        given(redisRepository.countWatchers(contentId)).willReturn(3L);
        given(contentStatsRepository.findById(contentId)).willReturn(Optional.empty());

        service.sync(contentId);

        then(contentStatsRepository).should(never())
                .updateViewerCount(eq(contentId), eq(3), any(Instant.class));
    }

    @Test
    void sync_capsViewerCountWhenRedisCountExceedsIntegerMax() {
        UUID contentId = UUID.randomUUID();
        given(redisRepository.countWatchers(contentId)).willReturn((long) Integer.MAX_VALUE + 1);
        given(contentStatsRepository.findById(contentId)).willReturn(Optional.of(stats(1)));
        given(contentStatsRepository.updateViewerCount(
                eq(contentId), eq(Integer.MAX_VALUE), any(Instant.class)))
                .willReturn(1);

        service.sync(contentId);

        then(contentStatsRepository).should()
                .updateViewerCount(eq(contentId), eq(Integer.MAX_VALUE), any(Instant.class));
    }

    private ContentStats stats(int viewerCount) {
        Content content = Content.builder()
                .type(ContentType.MOVIE)
                .title("콘텐츠")
                .externalId(UUID.randomUUID().toString())
                .source("test")
                .build();
        return ContentStats.builder()
                .content(content)
                .viewerCount(viewerCount)
                .build();
    }
}
