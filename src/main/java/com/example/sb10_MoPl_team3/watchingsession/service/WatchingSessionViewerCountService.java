package com.example.sb10_MoPl_team3.watchingsession.service;

import com.example.sb10_MoPl_team3.content.repository.ContentStatsRepository;
import com.example.sb10_MoPl_team3.watchingsession.repository.WatchingSessionRedisRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WatchingSessionViewerCountService {

    private final WatchingSessionRedisRepository redisRepository;
    private final ContentStatsRepository contentStatsRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sync(UUID contentId) {
        long watcherCount = redisRepository.countWatchers(contentId);
        int viewerCount = toViewerCount(watcherCount);
        int updated = contentStatsRepository.updateViewerCount(contentId, viewerCount, Instant.now());
        if (updated != 1) {
            log.warn("시청자 수 보정 대상 콘텐츠 통계가 없습니다. contentId={}, viewerCount={}",
                    contentId, viewerCount);
        }
    }

    private int toViewerCount(long watcherCount) {
        if (watcherCount > Integer.MAX_VALUE) {
            log.warn("시청자 수가 int 범위를 초과해 최대값으로 보정합니다. watcherCount={}", watcherCount);
            return Integer.MAX_VALUE;
        }
        return (int) watcherCount;
    }
}
