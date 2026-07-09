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
public class TmdbBatchScheduler {

  private final JobLauncher jobLauncher;

  @Qualifier("tmdbMovieSyncJob")
  private final Job tmdbMovieSyncJob;

  @Qualifier("tmdbTvSyncJob")
  private final Job tmdbTvSyncJob;

  @Scheduled(cron = "${batch.schedule.tmdb-movie:0 0 3 * * *}")
  public void runTmdbMovieSyncJob() {
    run(tmdbMovieSyncJob);
  }

  @Scheduled(cron = "${batch.schedule.tmdb-tv:0 15 3 * * *}")
  public void runTmdbTvSyncJob() {
    run(tmdbTvSyncJob);
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
