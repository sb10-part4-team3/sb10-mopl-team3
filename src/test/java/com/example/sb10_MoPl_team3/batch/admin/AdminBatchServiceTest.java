package com.example.sb10_MoPl_team3.batch.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.example.sb10_MoPl_team3.batch.admin.dto.BatchJobExecutionPageResponse;
import com.example.sb10_MoPl_team3.batch.admin.dto.BatchJobSummaryDto;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.NoSuchJobException;

@ExtendWith(MockitoExtension.class)
class AdminBatchServiceTest {

  @Mock
  private JobExplorer jobExplorer;

  private AdminBatchService adminBatchService;

  private static final String JOB_NAME = "tmdbMovieSyncJob";

  @org.junit.jupiter.api.BeforeEach
  void setUp() {
    adminBatchService = new AdminBatchService(jobExplorer);
  }

  @Test
  @DisplayName("등록된 Job 중 실행 이력이 없는 Job은 상태 없이 요약된다")
  void listJobs_notExecuted() {
    given(jobExplorer.getLastJobInstance(org.mockito.ArgumentMatchers.anyString()))
        .willReturn(null);

    List<BatchJobSummaryDto> summaries = adminBatchService.listJobs();

    assertThat(summaries).hasSize(BatchJobRegistry.JOB_NAMES.size());
    assertThat(summaries).allSatisfy(summary -> {
      assertThat(summary.status()).isNull();
      assertThat(summary.lastExecutionId()).isNull();
    });
  }

  @Test
  @DisplayName("실행 이력이 있는 Job은 최근 실행 상태로 요약된다")
  void listJobs_withExecution() {
    JobInstance instance = new JobInstance(1L, JOB_NAME);
    JobExecution execution = new JobExecution(instance, 100L, new JobParameters());
    execution.setStatus(BatchStatus.COMPLETED);
    execution.setExitStatus(ExitStatus.COMPLETED);
    execution.setStartTime(LocalDateTime.of(2026, 7, 9, 3, 0));
    execution.setEndTime(LocalDateTime.of(2026, 7, 9, 3, 5));

    given(jobExplorer.getLastJobInstance(JOB_NAME)).willReturn(instance);
    given(jobExplorer.getLastJobExecution(instance)).willReturn(execution);
    given(jobExplorer.getLastJobInstance(org.mockito.ArgumentMatchers.argThat(
        name -> !JOB_NAME.equals(name)))).willReturn(null);

    List<BatchJobSummaryDto> summaries = adminBatchService.listJobs();

    BatchJobSummaryDto tmdbMovieSummary = summaries.stream()
        .filter(summary -> summary.jobName().equals(JOB_NAME))
        .findFirst()
        .orElseThrow();

    assertThat(tmdbMovieSummary.lastExecutionId()).isEqualTo(100L);
    assertThat(tmdbMovieSummary.status()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(tmdbMovieSummary.exitCode()).isEqualTo(ExitStatus.COMPLETED.getExitCode());
  }

  @Test
  @DisplayName("등록되지 않은 Job 이름으로 이력을 조회하면 예외가 발생한다")
  void listExecutions_unknownJobName() {
    assertThatThrownBy(() -> adminBatchService.listExecutions("unknownJob", 0, 20))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  @DisplayName("실행된 적 없는 Job의 이력을 조회하면 빈 목록과 0건을 반환한다")
  void listExecutions_noInstances() throws NoSuchJobException {
    given(jobExplorer.getJobInstances(eq(JOB_NAME), anyInt(), anyInt()))
        .willReturn(List.of());
    given(jobExplorer.getJobInstanceCount(JOB_NAME))
        .willThrow(new NoSuchJobException("no job"));

    BatchJobExecutionPageResponse<?> response =
        adminBatchService.listExecutions(JOB_NAME, 0, 20);

    assertThat(response.content()).isEmpty();
    assertThat(response.hasNext()).isFalse();
    assertThat(response.totalCount()).isZero();
  }

  @Test
  @DisplayName("Job 실행 이력을 페이지 단위로 조회한다")
  void listExecutions_withInstances() throws NoSuchJobException {
    JobInstance instance = new JobInstance(1L, JOB_NAME);
    JobExecution execution = new JobExecution(instance, 100L, new JobParameters());
    execution.setStatus(BatchStatus.FAILED);
    execution.setExitStatus(ExitStatus.FAILED);

    given(jobExplorer.getJobInstances(eq(JOB_NAME), anyInt(), anyInt()))
        .willReturn(List.of(instance));
    given(jobExplorer.getLastJobExecution(instance)).willReturn(execution);
    given(jobExplorer.getJobInstanceCount(JOB_NAME)).willReturn(1L);

    BatchJobExecutionPageResponse<?> response =
        adminBatchService.listExecutions(JOB_NAME, 0, 20);

    assertThat(response.content()).hasSize(1);
    assertThat(response.totalCount()).isEqualTo(1L);
    assertThat(response.hasNext()).isFalse();
  }
}
