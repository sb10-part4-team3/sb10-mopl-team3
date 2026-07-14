package com.example.sb10_MoPl_team3.notification.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class NotificationKafkaConsumerConfig {

    @Bean
    public CommonErrorHandler notificationFanoutKafkaErrorHandler(
            KafkaOperations<Object, Object> kafkaOperations,
            @Value("${notification.kafka.fanout.retry.max-attempts:3}") long maxAttempts,
            @Value("${notification.kafka.fanout.retry.interval-ms:1000}") long intervalMillis
    ) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaOperations,
                (record, exception) -> new TopicPartition(
                        NotificationKafkaTopics.FANOUT_DLQ,
                        -1
                )
        );
        long retryAttempts = Math.max(0, maxAttempts - 1);
        return new DefaultErrorHandler(recoverer, new FixedBackOff(intervalMillis, retryAttempts));
    }
}
