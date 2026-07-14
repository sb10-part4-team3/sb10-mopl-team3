package com.example.sb10_MoPl_team3.global.sse;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class SseRedisPubSubConfigTest {

    @Test
    void sseRedisMessageListenerTaskExecutor_usesBoundedPool() {
        SseRedisPubSubConfig config = new SseRedisPubSubConfig(
                org.mockito.Mockito.mock(SseBroadcastSubscriber.class));

        ThreadPoolTaskExecutor executor =
                (ThreadPoolTaskExecutor) config.sseRedisMessageListenerTaskExecutor();

        assertThat(executor.getThreadNamePrefix()).isEqualTo("sse-redis-listener-");
        assertThat(executor.getCorePoolSize()).isEqualTo(2);
        assertThat(executor.getMaxPoolSize()).isEqualTo(4);
        assertThat(executor.getQueueCapacity()).isEqualTo(100);

        executor.shutdown();
    }

    @Test
    void sseRedisMessageListenerContainer_registersContainer() {
        SseRedisPubSubConfig config = new SseRedisPubSubConfig(
                org.mockito.Mockito.mock(SseBroadcastSubscriber.class));
        ThreadPoolTaskExecutor executor =
                (ThreadPoolTaskExecutor) config.sseRedisMessageListenerTaskExecutor();

        var container = config.sseRedisMessageListenerContainer(
                org.mockito.Mockito.mock(RedisConnectionFactory.class),
                executor);

        assertThat(container).isNotNull();
        executor.shutdown();
    }
}
