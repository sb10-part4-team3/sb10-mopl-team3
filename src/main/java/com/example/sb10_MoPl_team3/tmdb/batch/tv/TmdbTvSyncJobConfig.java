package com.example.sb10_MoPl_team3.tmdb.batch.tv;

import com.example.sb10_MoPl_team3.batch.BatchJobFailureListener;
import com.example.sb10_MoPl_team3.global.exception.TmdbApiException;
import com.example.sb10_MoPl_team3.tmdb.batch.TmdbContentItemWriter;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbTvPopularResponse.TmdbTvResult;
import com.example.sb10_MoPl_team3.tmdb.service.SyncPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class TmdbTvSyncJobConfig {

  private static final int CHUNK_SIZE = 20;

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final TmdbTvItemReader tmdbTvItemReader;
  private final TmdbTvPayloadProcessor tmdbTvPayloadProcessor;
  private final TmdbContentItemWriter tmdbContentItemWriter;
  private final BatchJobFailureListener batchJobFailureListener;

  @Bean
  public Job tmdbTvSyncJob() {
    return new JobBuilder("tmdbTvSyncJob", jobRepository)
        .start(tmdbTvFetchStep())
        .listener(batchJobFailureListener)
        .build();
  }

  @Bean
  public Step tmdbTvFetchStep() {
    return new StepBuilder("tmdbTvFetchStep", jobRepository)
        .<TmdbTvResult, SyncPayload>chunk(CHUNK_SIZE, transactionManager)
        .reader(tmdbTvItemReader)
        .processor(tmdbTvPayloadProcessor)
        .writer(tmdbContentItemWriter)
        .faultTolerant()
        .retryLimit(3)
        .retry(TmdbApiException.class)
        .skipLimit(50)
        .skip(TmdbApiException.class)
        .build();
  }
}