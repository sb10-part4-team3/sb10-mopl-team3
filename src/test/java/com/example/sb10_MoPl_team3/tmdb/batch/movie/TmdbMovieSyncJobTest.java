package com.example.sb10_MoPl_team3.tmdb.batch.movie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.example.sb10_MoPl_team3.tmdb.client.TmdbApiClient;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbGenreListResponse;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbGenreListResponse.TmdbGenre;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbMoviePopularResponse;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbMoviePopularResponse.TmdbMovieResult;
import java.util.List;
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
