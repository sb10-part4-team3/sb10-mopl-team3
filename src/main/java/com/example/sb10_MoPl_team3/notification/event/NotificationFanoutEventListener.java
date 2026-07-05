package com.example.sb10_MoPl_team3.notification.event;

import com.example.sb10_MoPl_team3.notification.service.NotificationFanoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationFanoutEventListener {

    private final NotificationFanoutService fanoutService;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(NotificationFanoutEvent event) {
        try {
            fanoutService.handle(event);
        } catch (RuntimeException exception) {
            log.error("알림 팬아웃 처리 실패: audienceType={}, sourceId={}",
                    event.audienceType(), event.sourceId(), exception);
        }
    }
}
