package com.example.sb10_MoPl_team3.batch.admin;

import com.example.sb10_MoPl_team3.batch.admin.dto.BatchJobExecutionDto;
import com.example.sb10_MoPl_team3.batch.admin.dto.BatchJobExecutionPageResponse;
import com.example.sb10_MoPl_team3.batch.admin.dto.BatchJobSummaryDto;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminBatchService {

  private static final int DEFAULT_SIZE = 20;
  private static final int MAX_SIZE = 100;

  private final JobExplorer jobExplorer;

  public List<BatchJobSummaryDto> listJobs() {
    return BatchJobRegistry.JOB_NAMES.stream()
        .map(this::toSummary)
        .toList();
  }

  public BatchJobExecutionPageResponse<BatchJobExecutionDto> listExecutions(
      String jobName, int page, int size) {
    validateJobName(jobName);
    int normalizedSize = normalizeSize(size);
    int normalizedPage = Math.max(page, 0);

    List<JobInstance> instances = jobExplorer.getJobInstances(
        jobName, normalizedPage * normalizedSize, normalizedSize + 1);
    boolean hasNext = instances.size() > normalizedSize;
    List<JobInstance> pageInstances = hasNext
        ? instances.subList(0, normalizedSize)
        : instances;

    List<BatchJobExecutionDto> content = pageInstances.stream()
        .map(instance -> BatchJobExecutionDto.from(instance, latestExecution(instance)))
        .toList();

    return new BatchJobExecutionPageResponse<>(
        content, normalizedPage, normalizedSize, hasNext, countInstances(jobName));
  }

  private BatchJobSummaryDto toSummary(String jobName) {
    JobInstance instance = jobExplorer.getLastJobInstance(jobName);
    if (instance == null) {
      return BatchJobSummaryDto.notExecuted(jobName);
    }
    return BatchJobSummaryDto.from(jobName, latestExecution(instance));
  }

  private JobExecution latestExecution(JobInstance instance) {
    JobExecution execution = jobExplorer.getLastJobExecution(instance);
    if (execution == null) {
      throw new IllegalStateException(
          "JobExecution을 찾을 수 없습니다. instanceId=" + instance.getInstanceId());
    }
    return execution;
  }

  private long countInstances(String jobName) {
    try {
      return jobExplorer.getJobInstanceCount(jobName);
    } catch (NoSuchJobException e) {
      return 0;
    }
  }

  private void validateJobName(String jobName) {
    if (!BatchJobRegistry.JOB_NAMES.contains(jobName)) {
      throw new BusinessException(ErrorCode.BATCH_JOB_NOT_FOUND);
    }
  }

  private int normalizeSize(int size) {
    if (size <= 0) {
      return DEFAULT_SIZE;
    }
    return Math.min(size, MAX_SIZE);
  }
}
