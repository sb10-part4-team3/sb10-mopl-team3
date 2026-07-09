package com.example.sb10_MoPl_team3.batch.admin.dto;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;

public record BatchJobExecutionDto(
    Long jobInstanceId,
    Long jobExecutionId,
    String jobName,
    BatchStatus status,
    String exitCode,
    String exitDescription,
    LocalDateTime startTime,
    LocalDateTime endTime,
    List<BatchStepExecutionDto> steps
) {

  public static BatchJobExecutionDto from(JobInstance instance, JobExecution execution) {
    List<BatchStepExecutionDto> steps = execution.getStepExecutions().stream()
        .map(BatchStepExecutionDto::from)
        .toList();

    return new BatchJobExecutionDto(
        instance.getInstanceId(),
        execution.getId(),
        instance.getJobName(),
        execution.getStatus(),
        execution.getExitStatus().getExitCode(),
        execution.getExitStatus().getExitDescription(),
        execution.getStartTime(),
        execution.getEndTime(),
        steps
    );
  }
}
