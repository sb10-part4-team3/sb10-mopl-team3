package com.example.sb10_MoPl_team3.tmdb.batch.movie;

import com.example.sb10_MoPl_team3.global.exception.TmdbApiException;
import com.example.sb10_MoPl_team3.tmdb.batch.TmdbContentItemWriter;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbMoviePopularResponse.TmdbMovieResult;
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
public class TmdbMovieSyncJobConfig {

  private static final int CHUNK_SIZE = 20;

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final TmdbMovieItemReader tmdbMovieItemReader;
  private final TmdbMoviePayloadProcessor tmdbMoviePayloadProcessor;
  private final TmdbContentItemWriter tmdbContentItemWriter;

  @Bean
  public Job tmdbMovieSyncJob() {
    return new JobBuilder("tmdbMovieSyncJob", jobRepository)
        .start(tmdbMovieFetchStep())
        .build();
  }

  @Bean
  public Step tmdbMovieFetchStep() {
    return new StepBuilder("tmdbMovieFetchStep", jobRepository)
        .<TmdbMovieResult, SyncPayload>chunk(CHUNK_SIZE, transactionManager)
        .reader(tmdbMovieItemReader)
        .processor(tmdbMoviePayloadProcessor)
        .writer(tmdbContentItemWriter)
        .faultTolerant()
        .retryLimit(3)
        .retry(TmdbApiException.class)
        .skipLimit(50)
        .skip(Exception.class)
        .build();
  }
}