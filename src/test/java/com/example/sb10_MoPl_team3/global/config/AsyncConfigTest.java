package com.example.sb10_MoPl_team3.global.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncConfigTest {

    @Test
    @DisplayName("DM 비동기 executor의 용량과 거부 정책을 설정한다")
    void directMessageExecutor_hasExpectedCapacityAndRejectionPolicy() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor)
                new AsyncConfig().directMessageExecutor();
        try {
            assertThat(executor.getCorePoolSize()).isEqualTo(2);
            assertThat(executor.getMaxPoolSize()).isEqualTo(4);
            assertThat(executor.getThreadPoolExecutor().getQueue().remainingCapacity())
                    .isEqualTo(100);
            assertThat(executor.getThreadPoolExecutor().getRejectedExecutionHandler())
                    .isInstanceOf(ThreadPoolExecutor.AbortPolicy.class);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("시청자 캐시 executor의 용량과 거부 정책을 설정한다")
    void watcherCacheExecutor_hasExpectedCapacityAndRejectionPolicy() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor)
                new AsyncConfig().watcherCacheExecutor();
        try {
            assertThat(executor.getCorePoolSize()).isEqualTo(1);
            assertThat(executor.getMaxPoolSize()).isEqualTo(2);
            assertThat(executor.getThreadPoolExecutor().getQueue().remainingCapacity())
                    .isEqualTo(100);
            assertThat(executor.getThreadPoolExecutor().getRejectedExecutionHandler())
                    .isInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class);
        } finally {
            executor.shutdown();
        }
    }
}
