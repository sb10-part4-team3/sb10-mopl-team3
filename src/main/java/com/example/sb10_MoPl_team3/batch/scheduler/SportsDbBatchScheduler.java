package com.example.sb10_MoPl_team3.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "batch.schedule", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SportsDbBatchScheduler {

  private final JobLauncher jobLauncher;

  @Qualifier("sportsDbNextEventsSyncJob")
  private final Job sportsDbNextEventsSyncJob;

  @Qualifier("sportsDbPastEventsSyncJob")
  private final Job sportsDbPastEventsSyncJob;

  @Scheduled(cron = "${batch.schedule.sportsdb-next:0 30 3 * * *}")
  public void runSportsDbNextEventsSyncJob() {
    run(sportsDbNextEventsSyncJob);
  }

  @Scheduled(cron = "${batch.schedule.sportsdb-past:0 45 3 * * *}")
  public void runSportsDbPastEventsSyncJob() {
    run(sportsDbPastEventsSyncJob);
  }

  private void run(Job job) {
    JobParameters params = new JobParametersBuilder()
        .addLong("time", System.currentTimeMillis())
        .toJobParameters();

    try {
      jobLauncher.run(job, params);
    } catch (Exception e) {
      log.error("배치 Job 실행 실패: jobName={}", job.getName(), e);
    }
  }
}
