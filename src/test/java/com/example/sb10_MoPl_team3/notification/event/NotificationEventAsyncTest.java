package com.example.sb10_MoPl_team3.notification.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sb10_MoPl_team3.global.config.AsyncConfig;
import com.example.sb10_MoPl_team3.notification.enums.NotificationLevel;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig({AsyncConfig.class, NotificationEventAsyncTest.TestConfig.class})
class NotificationEventAsyncTest {

    @Autowired
    private NotificationEventListener listener;

    @Autowired
    private RecordingNotificationEventHandler handler;

    @Test
    @DisplayName("알림 이벤트는 요청 스레드가 아닌 알림 전용 스레드에서 처리한다")
    void handle_runsOnNotificationExecutor() throws InterruptedException {
        String callerThread = Thread.currentThread().getName();
        NotificationEvent event = new NotificationEvent(
                UUID.randomUUID(), "제목", "내용", NotificationLevel.INFO);

        listener.handle(event);

        assertThat(handler.await()).isTrue();
        assertThat(handler.threadName()).startsWith("notification-");
        assertThat(handler.threadName()).isNotEqualTo(callerThread);
    }

    @Configuration
    @Import(NotificationEventListener.class)
    static class TestConfig {

        @Bean
        RecordingNotificationEventHandler recordingNotificationEventHandler() {
            return new RecordingNotificationEventHandler();
        }
    }

    static class RecordingNotificationEventHandler implements NotificationEventHandler {

        private final CountDownLatch latch = new CountDownLatch(1);
        private final AtomicReference<String> threadName = new AtomicReference<>();

        @Override
        public void handle(NotificationEvent event) {
            threadName.set(Thread.currentThread().getName());
            latch.countDown();
        }

        boolean await() throws InterruptedException {
            return latch.await(3, TimeUnit.SECONDS);
        }

        String threadName() {
            return threadName.get();
        }
    }
}
