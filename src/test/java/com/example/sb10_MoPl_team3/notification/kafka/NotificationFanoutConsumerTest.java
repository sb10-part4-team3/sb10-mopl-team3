package com.example.sb10_MoPl_team3.notification.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import com.example.sb10_MoPl_team3.notification.config.NotificationKafkaTopics;
import com.example.sb10_MoPl_team3.notification.enums.NotificationLevel;
import com.example.sb10_MoPl_team3.notification.event.NotificationAudienceType;
import com.example.sb10_MoPl_team3.notification.service.NotificationFanoutJobService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.annotation.KafkaListener;

@ExtendWith(MockitoExtension.class)
class NotificationFanoutConsumerTest {

    @Mock
    NotificationFanoutJobService jobService;

    @Test
    @DisplayName("Kafka 팬아웃 메시지를 job 서비스로 전달한다")
    void handle_delegatesToJobService() {
        NotificationFanoutKafkaMessage message = message();
        NotificationFanoutConsumer consumer = new NotificationFanoutConsumer(jobService);

        consumer.handle(message);

        then(jobService).should().process(message);
    }

    @Test
    @DisplayName("팬아웃 처리 실패는 Kafka 에러 핸들러가 처리할 수 있도록 전파한다")
    void handle_propagatesFanoutFailure() {
        NotificationFanoutKafkaMessage message = message();
        NotificationFanoutConsumer consumer = new NotificationFanoutConsumer(jobService);
        RuntimeException exception = new RuntimeException("fanout failed");
        willThrow(exception).given(jobService).process(message);

        assertThatThrownBy(() -> consumer.handle(message)).isSameAs(exception);
    }

    @Test
    @DisplayName("notification.fanout 토픽을 구독한다")
    void handle_listensFanoutTopic() throws NoSuchMethodException {
        KafkaListener kafkaListener = NotificationFanoutConsumer.class
                .getMethod("handle", NotificationFanoutKafkaMessage.class)
                .getAnnotation(KafkaListener.class);

        assertThat(kafkaListener).isNotNull();
        assertThat(kafkaListener.topics()).containsExactly(NotificationKafkaTopics.FANOUT);
        assertThat(kafkaListener.groupId())
                .isEqualTo("${notification.kafka.fanout.consumer-group:mopl-notification-fanout}");
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
