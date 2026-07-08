package com.example.sb10_MoPl_team3.tmdb.batch.tv;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@SpringBatchTest
class TmdbTvSyncJobTest {

  @Autowired
  private JobLauncher jobLauncher;

  @Autowired
  @Qualifier("tmdbTvSyncJob")
  private Job tmdbTvSyncJob;

  @Test
  void tmdbTvSyncJob_실행하면_COMPLETED_상태로_종료된다() throws Exception {
    // given
    JobParameters params = new JobParametersBuilder()
        .addLong("time", System.currentTimeMillis())
        .toJobParameters();

    // when
    JobExecution execution = jobLauncher.run(tmdbTvSyncJob, params);

    // then
    System.out.println("Job status: " + execution.getStatus());
    System.out.println("Exit status: " + execution.getExitStatus());
    assertThat(execution.getStatus().toString()).isEqualTo("COMPLETED");
  }
}