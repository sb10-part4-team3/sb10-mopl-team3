package com.example.sb10_MoPl_team3.notification.kafka;

import com.example.sb10_MoPl_team3.notification.config.NotificationKafkaTopics;
import com.example.sb10_MoPl_team3.notification.service.NotificationFanoutDlqService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationFanoutDlqConsumer {

    private final NotificationFanoutDlqService dlqService;

    @KafkaListener(
            topics = NotificationKafkaTopics.FANOUT_DLQ,
            groupId = "${notification.kafka.fanout.dlq.consumer-group:mopl-notification-fanout-dlq}"
    )
    public void handle(
            NotificationFanoutKafkaMessage message,
            @Header(name = KafkaHeaders.DLT_EXCEPTION_MESSAGE, required = false)
            String errorMessage
    ) {
        log.error(
                "알림 팬아웃 DLQ 메시지 수신: outboxId={}, audienceType={}, sourceId={}, errorMessage={}",
                message.outboxId(),
                message.audienceType(),
                message.sourceId(),
                errorMessage
        );
        dlqService.save(message, errorMessage);
    }
}
