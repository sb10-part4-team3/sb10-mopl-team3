package com.example.sb10_MoPl_team3.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.given;

import com.example.sb10_MoPl_team3.notification.entity.NotificationFanoutOutbox;
import com.example.sb10_MoPl_team3.notification.enums.NotificationFanoutOutboxStatus;
import com.example.sb10_MoPl_team3.notification.enums.NotificationLevel;
import com.example.sb10_MoPl_team3.notification.event.NotificationAudienceType;
import com.example.sb10_MoPl_team3.notification.event.NotificationFanoutEvent;
import com.example.sb10_MoPl_team3.notification.repository.NotificationFanoutOutboxRepository;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationFanoutOutboxServiceTest {

    @Mock
    NotificationFanoutOutboxRepository repository;

    @InjectMocks
    NotificationFanoutOutboxService service;

    @Test
    @DisplayName("팬아웃 이벤트를 PENDING outbox로 저장한다")
    void save_storesPendingOutbox() {
        NotificationFanoutEvent event = event();
        given(repository.save(org.mockito.ArgumentMatchers.any(NotificationFanoutOutbox.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        NotificationFanoutOutbox saved = service.save(event);

        ArgumentCaptor<NotificationFanoutOutbox> captor =
                ArgumentCaptor.forClass(NotificationFanoutOutbox.class);
        then(repository).should().save(captor.capture());
        assertThat(captor.getValue().toEvent()).isEqualTo(event);
        assertThat(captor.getValue().getStatus())
                .isEqualTo(NotificationFanoutOutboxStatus.PENDING);
        assertThat(saved.toEvent()).isEqualTo(event);
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
