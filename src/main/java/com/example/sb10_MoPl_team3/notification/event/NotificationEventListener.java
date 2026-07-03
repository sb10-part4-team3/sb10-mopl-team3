package com.example.sb10_MoPl_team3.notification.event;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final List<NotificationEventHandler> handlers;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(NotificationEvent event) {
        for (NotificationEventHandler handler : handlers) {
            try {
                handler.handle(event);
            } catch (RuntimeException exception) {
                log.error(
                        "알림 이벤트 처리 실패: receiverId={}, handler={}",
                        event.receiverId(),
                        handler.getClass().getName(),
                        exception
                );
            }
        }
    }
}
