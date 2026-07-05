package com.example.sb10_MoPl_team3.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.sb10_MoPl_team3.notification.entity.Notification;
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

@ExtendWith(MockitoExtension.class)
class NotificationFanoutBatchServiceTest {

    @Mock UserRepository userRepository;
    @Mock NotificationRepository notificationRepository;
    @InjectMocks NotificationFanoutBatchService batchService;

    @Test
    void saveBatch_savesNotificationsAndReturnsSavedCount() {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        given(userRepository.findAllById(List.of(firstId, secondId)))
                .willReturn(List.of(user("first@test.com"), user("second@test.com")));

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
        assertThat(captor.getValue()).hasSize(2);
        assertThat(savedCount).isEqualTo(2);
    }

    private User user(String email) {
        return new User(email, "사용자", "password", null, UserRole.USER);
    }
}
