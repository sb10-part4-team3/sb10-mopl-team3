package com.example.sb10_MoPl_team3.sportsdb.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class SportsDbEventsSyncJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager platformTransactionManager;
  private final SportsDbNextEventsSyncTasklet sportsDbNextEventsSyncTasklet;
  private final SportsDbPastEventsSyncTasklet sportsDbPastEventsSyncTasklet;

  @Bean
  public Job sportsDbNextEventsSyncJob() {
    return new JobBuilder("sportsDbNextEventsSyncJob", jobRepository)
        .start(sportsDbNextEventsSyncStep())
        .incrementer(new RunIdIncrementer())
        .build();
  }

  @Bean
  public Step sportsDbNextEventsSyncStep() {
    return new StepBuilder("sportsDbNextEventsSyncStep", jobRepository)
        .tasklet(sportsDbNextEventsSyncTasklet, platformTransactionManager)
        .build();

  }

  @Bean
  public Job sportsDbPastEventsSyncJob() {
    return new JobBuilder("sportsDbPastEventsSyncJob", jobRepository)
        .start(sportsDbPastEventsSyncStep())
        .incrementer(new RunIdIncrementer())
        .build();
  }

  @Bean
  public Step sportsDbPastEventsSyncStep() {
    return new StepBuilder("sportsDbPastEventsSyncStep", jobRepository)
        .tasklet(sportsDbPastEventsSyncTasklet, platformTransactionManager)
        .build();

  }
}
