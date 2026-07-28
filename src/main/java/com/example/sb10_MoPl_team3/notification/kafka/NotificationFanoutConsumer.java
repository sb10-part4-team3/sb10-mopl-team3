package com.example.sb10_MoPl_team3.notification.kafka;

import com.example.sb10_MoPl_team3.notification.config.NotificationKafkaTopics;
import com.example.sb10_MoPl_team3.notification.service.NotificationFanoutJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationFanoutConsumer {

    private final NotificationFanoutJobService jobService;

    @KafkaListener(
            topics = NotificationKafkaTopics.FANOUT,
            groupId = "${notification.kafka.fanout.consumer-group:mopl-notification-fanout}"
    )
    public void handle(NotificationFanoutKafkaMessage message) {
        log.info(
                "알림 팬아웃 Kafka 메시지 수신: outboxId={}, audienceType={}, sourceId={}",
                message.outboxId(),
                message.audienceType(),
                message.sourceId()
        );
        jobService.process(message);
    }
}
