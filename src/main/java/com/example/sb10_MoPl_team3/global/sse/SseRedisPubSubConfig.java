package com.example.sb10_MoPl_team3.global.sse;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "sse.redis.pubsub.enabled", havingValue = "true", matchIfMissing = true)
public class SseRedisPubSubConfig {

    private final SseBroadcastSubscriber sseBroadcastSubscriber;

    @Bean
    public TaskExecutor sseRedisMessageListenerTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("sse-redis-listener-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.initialize();
        return executor;
    }

    @Bean
    public RedisMessageListenerContainer sseRedisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            @Qualifier("sseRedisMessageListenerTaskExecutor")
            TaskExecutor sseRedisMessageListenerTaskExecutor
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setTaskExecutor(sseRedisMessageListenerTaskExecutor);
        container.addMessageListener(
                sseBroadcastSubscriber,
                new ChannelTopic(SseEventPublisher.BROADCAST_CHANNEL));
        return container;
    }
}
