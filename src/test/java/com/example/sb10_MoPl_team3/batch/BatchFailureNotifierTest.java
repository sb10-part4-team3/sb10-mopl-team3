package com.example.sb10_MoPl_team3.batch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.sb10_MoPl_team3.notification.enums.NotificationLevel;
import com.example.sb10_MoPl_team3.notification.event.NotificationEvent;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BatchFailureNotifierTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  private BatchFailureNotifier batchFailureNotifier;

  @org.junit.jupiter.api.BeforeEach
  void setUp() {
    batchFailureNotifier = new BatchFailureNotifier(userRepository, eventPublisher);
  }

  @Test
  @DisplayName("관리자 전원에게 배치 실패 알림 이벤트를 발행한다")
  void notifyAdmins_publishesEventPerAdmin() {
    User admin1 = new User("admin1@test.com", "Admin1", "pw", null, UserRole.ADMIN);
    User admin2 = new User("admin2@test.com", "Admin2", "pw", null, UserRole.ADMIN);
    ReflectionTestUtils.setField(admin1, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(admin2, "id", UUID.randomUUID());
    given(userRepository.findByRole(UserRole.ADMIN)).willReturn(List.of(admin1, admin2));

    JobInstance instance = new JobInstance(1L, "tmdbMovieSyncJob");
    JobExecution execution = new JobExecution(instance, 100L, new JobParameters());

    batchFailureNotifier.notifyAdmins(execution);

    ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
    then(eventPublisher).should(org.mockito.Mockito.times(2)).publishEvent(captor.capture());

    List<NotificationEvent> events = captor.getAllValues();
    org.assertj.core.api.Assertions.assertThat(events)
        .extracting(NotificationEvent::level)
        .containsOnly(NotificationLevel.ERROR);
    org.assertj.core.api.Assertions.assertThat(events)
        .extracting(NotificationEvent::content)
        .allSatisfy(content -> org.assertj.core.api.Assertions.assertThat(content)
            .contains("tmdbMovieSyncJob"));
  }

  @Test
  @DisplayName("관리자가 없으면 알림을 발행하지 않는다")
  void notifyAdmins_noAdmins() {
    given(userRepository.findByRole(UserRole.ADMIN)).willReturn(List.of());

    JobInstance instance = new JobInstance(1L, "tmdbMovieSyncJob");
    JobExecution execution = new JobExecution(instance, 100L, new JobParameters());

    batchFailureNotifier.notifyAdmins(execution);

    then(eventPublisher).should(org.mockito.Mockito.never()).publishEvent(any());
  }
}
