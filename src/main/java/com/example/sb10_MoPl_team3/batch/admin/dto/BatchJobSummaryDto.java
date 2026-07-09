package com.example.sb10_MoPl_team3.batch.admin.dto;

import java.time.LocalDateTime;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;

public record BatchJobSummaryDto(
    String jobName,
    Long lastExecutionId,
    BatchStatus status,
    String exitCode,
    LocalDateTime startTime,
    LocalDateTime endTime
) {

  public static BatchJobSummaryDto notExecuted(String jobName) {
    return new BatchJobSummaryDto(jobName, null, null, null, null, null);
  }

  public static BatchJobSummaryDto from(String jobName, JobExecution execution) {
    return new BatchJobSummaryDto(
        jobName,
        execution.getId(),
        execution.getStatus(),
        execution.getExitStatus().getExitCode(),
        execution.getStartTime(),
        execution.getEndTime()
    );
  }
}
