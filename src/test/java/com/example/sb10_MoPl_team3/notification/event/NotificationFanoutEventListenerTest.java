package com.example.sb10_MoPl_team3.notification.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import com.example.sb10_MoPl_team3.notification.enums.NotificationLevel;
import com.example.sb10_MoPl_team3.notification.service.NotificationFanoutOutboxService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@ExtendWith(MockitoExtension.class)
class NotificationFanoutEventListenerTest {

    @Mock
    NotificationFanoutOutboxService outboxService;

    @Test
    @DisplayName("팬아웃 이벤트를 직접 처리하지 않고 outbox에 저장한다")
    void handle_savesOutbox() {
        NotificationFanoutEvent event = event();
        NotificationFanoutEventListener listener =
                new NotificationFanoutEventListener(outboxService);

        listener.handle(event);

        then(outboxService).should().save(event);
    }

    @Test
    @DisplayName("outbox 저장 실패가 호출자에게 전파되지 않는다")
    void handle_isolatesOutboxFailure() {
        NotificationFanoutEvent event = event();
        NotificationFanoutEventListener listener =
                new NotificationFanoutEventListener(outboxService);
        willThrow(new RuntimeException("outbox 저장 실패"))
                .given(outboxService).save(event);

        listener.handle(event);

        then(outboxService).should().save(event);
    }

    @Test
    @DisplayName("팬아웃 이벤트 리스너는 알림 전용 executor와 커밋 이후 이벤트를 사용한다")
    void handle_usesNotificationExecutorAfterCommit() throws NoSuchMethodException {
        Async async = NotificationFanoutEventListener.class
                .getMethod("handle", NotificationFanoutEvent.class)
                .getAnnotation(Async.class);
        TransactionalEventListener transactional = NotificationFanoutEventListener.class
                .getMethod("handle", NotificationFanoutEvent.class)
                .getAnnotation(TransactionalEventListener.class);

        assertThat(async).isNotNull();
        assertThat(async.value()).isEqualTo("notificationExecutor");
        assertThat(transactional).isNotNull();
        assertThat(transactional.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    }

    private NotificationFanoutEvent event() {
        return new NotificationFanoutEvent(
                NotificationAudienceType.FOLLOWERS,
                UUID.randomUUID(),
                "시청 시작",
                "새로운 활동입니다.",
                NotificationLevel.INFO
        );
    }
}
