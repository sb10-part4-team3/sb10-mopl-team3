package com.example.sb10_MoPl_team3.notification.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

import com.example.sb10_MoPl_team3.notification.config.NotificationKafkaTopics;
import com.example.sb10_MoPl_team3.notification.enums.NotificationLevel;
import com.example.sb10_MoPl_team3.notification.event.NotificationAudienceType;
import com.example.sb10_MoPl_team3.notification.service.NotificationFanoutDlqService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.annotation.KafkaListener;

@ExtendWith(MockitoExtension.class)
class NotificationFanoutDlqConsumerTest {

    @Mock
    NotificationFanoutDlqService dlqService;

    @Test
    @DisplayName("DLQ 메시지를 DB에 저장한다")
    void handle_savesDlqMessage() {
        NotificationFanoutKafkaMessage message = message();
        NotificationFanoutDlqConsumer consumer = new NotificationFanoutDlqConsumer(dlqService);

        consumer.handle(message, "fanout failed");

        then(dlqService).should().save(message, "fanout failed");
    }

    @Test
    @DisplayName("notification.fanout.dlq 토픽을 구독한다")
    void handle_listensDlqTopic() throws NoSuchMethodException {
        KafkaListener kafkaListener = NotificationFanoutDlqConsumer.class
                .getMethod("handle", NotificationFanoutKafkaMessage.class, String.class)
                .getAnnotation(KafkaListener.class);

        assertThat(kafkaListener).isNotNull();
        assertThat(kafkaListener.topics()).containsExactly(NotificationKafkaTopics.FANOUT_DLQ);
    }

    private NotificationFanoutKafkaMessage message() {
        return new NotificationFanoutKafkaMessage(
                UUID.randomUUID(),
                NotificationAudienceType.FOLLOWERS,
                UUID.randomUUID(),
                "시청 시작",
                "새로운 활동입니다.",
                NotificationLevel.INFO
        );
    }
}
