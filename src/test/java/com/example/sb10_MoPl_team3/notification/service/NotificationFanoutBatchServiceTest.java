package com.example.sb10_MoPl_team3.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.example.sb10_MoPl_team3.notification.entity.Notification;
import com.example.sb10_MoPl_team3.global.sse.SseEventPublisher;
import com.example.sb10_MoPl_team3.notification.dto.NotificationDto;
import com.example.sb10_MoPl_team3.notification.enums.NotificationLevel;
import com.example.sb10_MoPl_team3.notification.event.NotificationAudienceType;
import com.example.sb10_MoPl_team3.notification.event.NotificationFanoutEvent;
import com.example.sb10_MoPl_team3.notification.repository.NotificationRepository;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationFanoutBatchServiceTest {

    @Mock UserRepository userRepository;
    @Mock NotificationRepository notificationRepository;
    @Mock SseEventPublisher sseEventPublisher;
    @InjectMocks NotificationFanoutBatchService batchService;

    @Test
    void saveBatch_returnsZeroWithoutRepositoryCallsWhenReceiverIdsAreEmpty() {
        int savedCount = batchService.saveBatch(
                List.of(),
                new NotificationFanoutEvent(
                        NotificationAudienceType.FOLLOWERS,
                        UUID.randomUUID(),
                        "제목",
                        "내용",
                        NotificationLevel.INFO));

        assertThat(savedCount).isZero();
        then(userRepository).shouldHaveNoInteractions();
        then(notificationRepository).shouldHaveNoInteractions();
    }

    @Test
    void saveBatch_savesNotificationsAndReturnsSavedCount() {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        User first = user("first@test.com");
        User second = user("second@test.com");
        ReflectionTestUtils.setField(first, "id", firstId);
        ReflectionTestUtils.setField(second, "id", secondId);
        given(userRepository.findAllById(List.of(firstId, secondId)))
                .willReturn(List.of(first, second));
        given(notificationRepository.saveAll(org.mockito.ArgumentMatchers.anyList()))
                .willAnswer(invocation -> invocation.getArgument(0));

        int savedCount = batchService.saveBatch(
                List.of(firstId, secondId),
                new NotificationFanoutEvent(
                        NotificationAudienceType.FOLLOWERS,
                        UUID.randomUUID(),
                        "제목",
                        "내용",
                        NotificationLevel.INFO));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        then(notificationRepository).should().saveAll(captor.capture());
        List<Notification> notifications = captor.getValue();
        assertThat(notifications).hasSize(2);
        assertThat(notifications).extracting(Notification::getReceiver)
                .containsExactly(first, second);
        assertThat(notifications).extracting(Notification::getTitle)
                .containsOnly("제목");
        assertThat(notifications).extracting(Notification::getContent)
                .containsOnly("내용");
        assertThat(notifications).extracting(Notification::getLevel)
                .containsOnly(NotificationLevel.INFO);
        ArgumentCaptor<UUID> receiverIdCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<NotificationDto> dtoCaptor =
                ArgumentCaptor.forClass(NotificationDto.class);
        then(sseEventPublisher).should(times(2)).publishAfterCommit(
                receiverIdCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(SseEventPublisher.NOTIFICATIONS_EVENT),
                dtoCaptor.capture());
        assertThat(receiverIdCaptor.getAllValues()).containsExactly(firstId, secondId);
        assertThat(dtoCaptor.getAllValues()).extracting(NotificationDto::receiverId)
                .containsExactly(firstId, secondId);
        assertThat(dtoCaptor.getAllValues()).extracting(NotificationDto::title)
                .containsOnly("제목");
        assertThat(dtoCaptor.getAllValues()).extracting(NotificationDto::content)
                .containsOnly("내용");
        assertThat(dtoCaptor.getAllValues()).extracting(NotificationDto::level)
                .containsOnly(NotificationLevel.INFO);
        assertThat(savedCount).isEqualTo(2);
    }

    private User user(String email) {
        return new User(email, "사용자", "password", null, UserRole.USER);
    }
}
