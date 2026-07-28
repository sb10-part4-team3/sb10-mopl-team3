package com.example.sb10_MoPl_team3.batch;

import com.example.sb10_MoPl_team3.notification.enums.NotificationLevel;
import com.example.sb10_MoPl_team3.notification.event.NotificationEvent;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import org.springframework.batch.core.JobExecution;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BatchFailureNotifier {

  private static final String TITLE = "배치 작업 실패";

  private final UserRepository userRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public void notifyAdmins(JobExecution jobExecution) {
    String content = "%s 실행이 실패했습니다. (executionId=%d)".formatted(
        jobExecution.getJobInstance().getJobName(),
        jobExecution.getId());

    for (User admin : userRepository.findByRole(UserRole.ADMIN)) {
      eventPublisher.publishEvent(new NotificationEvent(
          admin.getId(),
          TITLE,
          content,
          NotificationLevel.ERROR));
    }
  }
}
