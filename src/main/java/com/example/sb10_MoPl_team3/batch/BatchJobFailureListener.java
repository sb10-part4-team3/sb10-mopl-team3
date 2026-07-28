package com.example.sb10_MoPl_team3.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchJobFailureListener implements JobExecutionListener {

  private final BatchFailureNotifier batchFailureNotifier;

  @Override
  public void afterJob(JobExecution jobExecution) {
    if (jobExecution.getStatus() != BatchStatus.FAILED) {
      return;
    }

    try {
      batchFailureNotifier.notifyAdmins(jobExecution);
    } catch (RuntimeException e) {
      log.error("배치 실패 알림 발송 실패: jobName={}, executionId={}",
          jobExecution.getJobInstance().getJobName(), jobExecution.getId(), e);
    }
  }
}
