package com.example.sb10_MoPl_team3.tmdb.batch.movie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.sb10_MoPl_team3.global.exception.TmdbApiException;
import com.example.sb10_MoPl_team3.tmdb.client.TmdbApiClient;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbGenreListResponse;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbGenreListResponse.TmdbGenre;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbMoviePopularResponse;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbMoviePopularResponse.TmdbMovieResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
class TmdbMovieSyncJobTest {

  @Autowired
  private JobLauncher jobLauncher;

  @Autowired
  @Qualifier("tmdbMovieSyncJob")
  private Job tmdbMovieSyncJob;

  @MockitoBean
  private TmdbApiClient tmdbApiClient;

  @Test
  void tmdbMovieSyncJob_실행하면_COMPLETED_상태로_종료된다() throws Exception {
    // given
    given(tmdbApiClient.getPopularMovies(1)).willReturn(samplePopularMoviesResponse());
    given(tmdbApiClient.getMovieGenres()).willReturn(sampleGenreResponse());

    JobParameters params = new JobParametersBuilder()
        .addLong("time", System.currentTimeMillis())
        .toJobParameters();

    // when
    JobExecution execution = jobLauncher.run(tmdbMovieSyncJob, params);

    // then
    assertThat(execution.getStatus().toString()).isEqualTo("COMPLETED");
  }

  @Test
  void tmdbMovieSyncJob_API_실패_후_재시도로_성공한다() throws Exception {
    // given: 처음 두 번은 실패, 세 번째(retryLimit=3 이내)는 성공
    given(tmdbApiClient.getPopularMovies(1))
        .willThrow(new TmdbApiException("일시적 오류"))
        .willThrow(new TmdbApiException("일시적 오류"))
        .willReturn(samplePopularMoviesResponse());
    given(tmdbApiClient.getMovieGenres()).willReturn(sampleGenreResponse());

    JobParameters params = new JobParametersBuilder()
        .addLong("time", System.currentTimeMillis())
        .toJobParameters();

    // when
    JobExecution execution = jobLauncher.run(tmdbMovieSyncJob, params);

    // then
    assertThat(execution.getStatus().toString()).isEqualTo("COMPLETED");
    then(tmdbApiClient).should(Mockito.times(3)).getPopularMovies(1);
  }

  @Test
  void tmdbMovieSyncJob_재시도_한도를_초과하면_FAILED로_종료된다() throws Exception {
    // given: retryLimit(3)을 넘어서까지 계속 실패.
    // reader 단계의 실패는 skip(TmdbApiException.class)으로 건너뛰어지지 않고
    // Job이 FAILED로 종료된다 (동기화를 부분 성공으로 조용히 넘기지 않는 안전한 동작).
    given(tmdbApiClient.getPopularMovies(1))
        .willThrow(new TmdbApiException("영구 오류"));
    given(tmdbApiClient.getMovieGenres()).willReturn(sampleGenreResponse());

    JobParameters params = new JobParametersBuilder()
        .addLong("time", System.currentTimeMillis())
        .toJobParameters();

    // when
    JobExecution execution = jobLauncher.run(tmdbMovieSyncJob, params);

    // then
    assertThat(execution.getStatus().toString()).isEqualTo("FAILED");
    // 정확한 호출 횟수는 Spring Batch의 재시도/스킵 스캔 방식에 따라 달라지는 내부 구현 세부사항이라
    // 고정하지 않고, 단일 호출로 끝나지 않고 재시도가 실제로 여러 번 일어났다는 사실만 검증한다.
    then(tmdbApiClient).should(Mockito.atLeast(4)).getPopularMovies(1);
  }

  private TmdbMoviePopularResponse samplePopularMoviesResponse() {
    TmdbMovieResult result = new TmdbMovieResult(
        1L,
        "샘플 영화",
        "Sample Movie",
        "샘플 개요",
        "/poster.jpg",
        "/backdrop.jpg",
        "2024-01-01",
        10.0,
        7.5,
        100,
        List.of(28),
        false
    );
    return new TmdbMoviePopularResponse(1, List.of(result), 1, 1);
  }

  private TmdbGenreListResponse sampleGenreResponse() {
    return new TmdbGenreListResponse(List.of(new TmdbGenre(28, "액션")));
  }
}
