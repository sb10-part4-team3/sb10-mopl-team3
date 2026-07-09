package com.example.sb10_MoPl_team3.batch.admin.dto;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.StepExecution;

public record BatchStepExecutionDto(
    String stepName,
    BatchStatus status,
    long readCount,
    long writeCount,
    long skipCount
) {

  public static BatchStepExecutionDto from(StepExecution stepExecution) {
    return new BatchStepExecutionDto(
        stepExecution.getStepName(),
        stepExecution.getStatus(),
        stepExecution.getReadCount(),
        stepExecution.getWriteCount(),
        stepExecution.getSkipCount()
    );
  }
}
