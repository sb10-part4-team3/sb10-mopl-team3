package com.example.sb10_MoPl_team3.notification.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class NotificationKafkaTopicConfig {

    @Bean
    public NewTopic notificationFanoutTopic(
            @Value("${notification.kafka.fanout.partitions:3}") int partitions,
            @Value("${notification.kafka.fanout.replicas:1}") int replicas
    ) {
        return TopicBuilder.name(NotificationKafkaTopics.FANOUT)
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }

    @Bean
    public NewTopic notificationFanoutDlqTopic(
            @Value("${notification.kafka.fanout-dlq.partitions:1}") int partitions,
            @Value("${notification.kafka.fanout-dlq.replicas:1}") int replicas
    ) {
        return TopicBuilder.name(NotificationKafkaTopics.FANOUT_DLQ)
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }
}
