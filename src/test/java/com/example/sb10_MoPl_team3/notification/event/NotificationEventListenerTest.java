package com.example.sb10_MoPl_team3.notification.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import com.example.sb10_MoPl_team3.notification.enums.NotificationLevel;
import java.util.List;
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
class NotificationEventListenerTest {

    @Mock
    private NotificationEventHandler firstHandler;

    @Mock
    private NotificationEventHandler secondHandler;

    @Test
    @DisplayName("알림 이벤트를 등록된 모든 처리기에 전달한다")
    void handle_delegatesToAllHandlers() {
        NotificationEvent event = event();
        NotificationEventListener listener = new NotificationEventListener(
                List.of(firstHandler, secondHandler));

        listener.handle(event);

        then(firstHandler).should().handle(event);
        then(secondHandler).should().handle(event);
    }

    @Test
    @DisplayName("한 처리기의 실패가 다른 알림 처리기로 전파되지 않는다")
    void handle_isolatesHandlerFailure() {
        NotificationEvent event = event();
        NotificationEventListener listener = new NotificationEventListener(
                List.of(firstHandler, secondHandler));
        willThrow(new RuntimeException("알림 처리 실패"))
                .given(firstHandler).handle(event);

        listener.handle(event);

        then(secondHandler).should().handle(event);
    }

    @Test
    @DisplayName("알림 이벤트 리스너는 알림 전용 executor를 사용한다")
    void handle_usesNotificationExecutor() throws NoSuchMethodException {
        Async async = NotificationEventListener.class
                .getMethod("handle", NotificationEvent.class)
                .getAnnotation(Async.class);
        TransactionalEventListener transactional = NotificationEventListener.class
                .getMethod("handle", NotificationEvent.class)
                .getAnnotation(TransactionalEventListener.class);

        assertThat(async).isNotNull();
        assertThat(async.value()).isEqualTo("notificationExecutor");
        assertThat(transactional).isNotNull();
        assertThat(transactional.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    }

    private NotificationEvent event() {
        return new NotificationEvent(
                UUID.randomUUID(), "알림 제목", "알림 내용", NotificationLevel.INFO);
    }
}
