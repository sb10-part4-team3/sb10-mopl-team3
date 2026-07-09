package com.example.sb10_MoPl_team3.batch;

import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;

@ExtendWith(MockitoExtension.class)
class BatchJobFailureListenerTest {

  @Mock
  private BatchFailureNotifier batchFailureNotifier;

  private BatchJobFailureListener listener;

  @org.junit.jupiter.api.BeforeEach
  void setUp() {
    listener = new BatchJobFailureListener(batchFailureNotifier);
  }

  @Test
  @DisplayName("Job이 실패로 종료되면 관리자에게 알림을 발송한다")
  void afterJob_failed_notifiesAdmins() {
    JobInstance instance = new JobInstance(1L, "tmdbMovieSyncJob");
    JobExecution execution = new JobExecution(instance, 100L, new JobParameters());
    execution.setStatus(BatchStatus.FAILED);

    listener.afterJob(execution);

    then(batchFailureNotifier).should().notifyAdmins(execution);
  }

  @Test
  @DisplayName("Job이 정상 완료되면 알림을 발송하지 않는다")
  void afterJob_completed_doesNotNotify() {
    JobInstance instance = new JobInstance(1L, "tmdbMovieSyncJob");
    JobExecution execution = new JobExecution(instance, 100L, new JobParameters());
    execution.setStatus(BatchStatus.COMPLETED);

    listener.afterJob(execution);

    then(batchFailureNotifier).should(never()).notifyAdmins(execution);
  }

  @Test
  @DisplayName("알림 발송 중 예외가 발생해도 Job 종료 처리에 영향을 주지 않는다")
  void afterJob_notifierThrows_doesNotPropagate() {
    JobInstance instance = new JobInstance(1L, "tmdbMovieSyncJob");
    JobExecution execution = new JobExecution(instance, 100L, new JobParameters());
    execution.setStatus(BatchStatus.FAILED);

    willThrow(new RuntimeException("notify failed"))
        .given(batchFailureNotifier).notifyAdmins(execution);

    org.assertj.core.api.Assertions.assertThatCode(() -> listener.afterJob(execution))
        .doesNotThrowAnyException();
  }
}
