package com.example.sb10_MoPl_team3.global.sse;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "sse.redis.pubsub.enabled", havingValue = "true", matchIfMissing = true)
public class SseRedisPubSubConfig {

    private final SseBroadcastSubscriber sseBroadcastSubscriber;

    @Bean
    public RedisMessageListenerContainer sseRedisMessageListenerContainer(
            RedisConnectionFactory connectionFactory
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(
                sseBroadcastSubscriber,
                new ChannelTopic(SseEventPublisher.BROADCAST_CHANNEL));
        return container;
    }
}
