package com.example.sb10_MoPl_team3.watchingsession.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ThreadPoolExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class WatchingSessionRedisPubSubConfigTest {

    @Test
    void watchingSessionRedisMessageListenerTaskExecutor_usesBoundedPoolWithCallerRunsPolicy() {
        WatchingSessionRedisPubSubConfig config = new WatchingSessionRedisPubSubConfig(
                org.mockito.Mockito.mock(WatchingSessionBroadcastSubscriber.class));

        ThreadPoolTaskExecutor executor =
                (ThreadPoolTaskExecutor) config.watchingSessionRedisMessageListenerTaskExecutor();

        assertThat(executor.getThreadNamePrefix()).isEqualTo("watching-session-redis-listener-");
        assertThat(executor.getCorePoolSize()).isEqualTo(2);
        assertThat(executor.getMaxPoolSize()).isEqualTo(4);
        assertThat(executor.getQueueCapacity()).isEqualTo(100);
        assertThat(executor.getThreadPoolExecutor().getRejectedExecutionHandler())
                .isInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class);

        executor.shutdown();
    }

    @Test
    void watchingSessionRedisMessageListenerContainer_registersContainer() {
        WatchingSessionRedisPubSubConfig config = new WatchingSessionRedisPubSubConfig(
                org.mockito.Mockito.mock(WatchingSessionBroadcastSubscriber.class));
        ThreadPoolTaskExecutor executor =
                (ThreadPoolTaskExecutor) config.watchingSessionRedisMessageListenerTaskExecutor();

        var container = config.watchingSessionRedisMessageListenerContainer(
                org.mockito.Mockito.mock(RedisConnectionFactory.class),
                executor);

        assertThat(container).isNotNull();
        executor.shutdown();
    }
}
