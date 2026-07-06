package com.example.sb10_MoPl_team3.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.notification.entity.Notification;
import com.example.sb10_MoPl_team3.notification.enums.NotificationLevel;
import com.example.sb10_MoPl_team3.notification.event.NotificationEvent;
import com.example.sb10_MoPl_team3.notification.repository.NotificationRepository;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("알림 이벤트를 수신하면 알림 엔티티를 저장한다")
    void handle_persistsNotification() {
        UUID receiverId = UUID.randomUUID();
        User receiver = user();
        NotificationEvent event = new NotificationEvent(
                receiverId, "제목", "내용", NotificationLevel.INFO);
        given(userRepository.findById(receiverId)).willReturn(Optional.of(receiver));

        notificationService.handle(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        then(notificationRepository).should().save(captor.capture());
        assertThat(captor.getValue().getReceiver()).isEqualTo(receiver);
        assertThat(captor.getValue().getTitle()).isEqualTo("제목");
        assertThat(captor.getValue().getContent()).isEqualTo("내용");
        assertThat(captor.getValue().getLevel()).isEqualTo(NotificationLevel.INFO);
    }

    @Test
    @DisplayName("본인의 알림을 읽으면 읽음 상태로 변경한다")
    void read_marksNotificationAsRead() {
        UUID receiverId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        Notification notification = new Notification(
                user(), "제목", "내용", NotificationLevel.INFO);
        given(notificationRepository.findByIdAndReceiverId(notificationId, receiverId))
                .willReturn(Optional.of(notification));

        notificationService.read(receiverId, notificationId);

        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();
    }

    @Test
    @DisplayName("본인 소유가 아닌 알림은 읽음 처리할 수 없다")
    void read_rejectsUnknownOrOtherUsersNotification() {
        UUID receiverId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        given(notificationRepository.findByIdAndReceiverId(notificationId, receiverId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.read(receiverId, notificationId))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND));
        then(notificationRepository).should().findByIdAndReceiverId(notificationId, receiverId);
        then(notificationRepository).shouldHaveNoMoreInteractions();
    }

    private User user() {
        return new User("user@test.com", "사용자", "password", null, UserRole.USER);
    }
}
