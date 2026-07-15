package com.example.sb10_MoPl_team3.watchingsession.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "watching-session.redis.pubsub.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class WatchingSessionRedisPubSubConfig {

    private final WatchingSessionBroadcastSubscriber watchingSessionBroadcastSubscriber;

    @Bean
    public TaskExecutor watchingSessionRedisMessageListenerTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("watching-session-redis-listener-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.initialize();
        return executor;
    }

    @Bean
    public RedisMessageListenerContainer watchingSessionRedisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            @Qualifier("watchingSessionRedisMessageListenerTaskExecutor")
            TaskExecutor watchingSessionRedisMessageListenerTaskExecutor
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setTaskExecutor(watchingSessionRedisMessageListenerTaskExecutor);
        container.addMessageListener(
                watchingSessionBroadcastSubscriber,
                new ChannelTopic(WatchingSessionBroadcastPublisher.BROADCAST_CHANNEL));
        return container;
    }
}
