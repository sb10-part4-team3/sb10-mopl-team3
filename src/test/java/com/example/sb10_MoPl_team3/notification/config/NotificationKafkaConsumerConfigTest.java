package com.example.sb10_MoPl_team3.notification.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DefaultErrorHandler;

class NotificationKafkaConsumerConfigTest {

    @Test
    void notificationFanoutKafkaErrorHandler_createsDefaultErrorHandler() {
        NotificationKafkaConsumerConfig config = new NotificationKafkaConsumerConfig();

        var errorHandler = config.notificationFanoutKafkaErrorHandler(
                org.mockito.Mockito.mock(KafkaOperations.class),
                3,
                1000);

        assertThat(errorHandler).isInstanceOf(DefaultErrorHandler.class);
    }

    @Test
    void notificationFanoutKafkaErrorHandler_allowsZeroRetryAttempts() {
        NotificationKafkaConsumerConfig config = new NotificationKafkaConsumerConfig();

        var errorHandler = config.notificationFanoutKafkaErrorHandler(
                org.mockito.Mockito.mock(KafkaOperations.class),
                0,
                1000);

        assertThat(errorHandler).isInstanceOf(DefaultErrorHandler.class);
    }
}
