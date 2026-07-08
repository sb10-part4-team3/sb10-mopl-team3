package com.example.sb10_MoPl_team3.tmdb.batch.tv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.example.sb10_MoPl_team3.tmdb.client.TmdbApiClient;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbGenreListResponse;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbGenreListResponse.TmdbGenre;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbTvPopularResponse;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbTvPopularResponse.TmdbTvResult;
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
class TmdbTvSyncJobTest {

  @Autowired
  private JobLauncher jobLauncher;

  @Autowired
  @Qualifier("tmdbTvSyncJob")
  private Job tmdbTvSyncJob;

  @MockitoBean
  private TmdbApiClient tmdbApiClient;

  @Test
  void tmdbTvSyncJob_실행하면_COMPLETED_상태로_종료된다() throws Exception {
    // given
    given(tmdbApiClient.getPopularTvs(1)).willReturn(samplePopularTvsResponse());
    given(tmdbApiClient.getTvGenres()).willReturn(sampleGenreResponse());

    JobParameters params = new JobParametersBuilder()
        .addLong("time", System.currentTimeMillis())
        .toJobParameters();

    // when
    JobExecution execution = jobLauncher.run(tmdbTvSyncJob, params);

    // then
    assertThat(execution.getStatus().toString()).isEqualTo("COMPLETED");
  }

  private TmdbTvPopularResponse samplePopularTvsResponse() {
    TmdbTvResult result = new TmdbTvResult(
        1L,
        "샘플 드라마",
        "Sample TV",
        "샘플 개요",
        "/poster.jpg",
        "/backdrop.jpg",
        "2024-01-01",
        10.0,
        7.5,
        100,
        List.of(18),
        List.of("US")
    );
    return new TmdbTvPopularResponse(1, List.of(result), 1, 1);
  }

  private TmdbGenreListResponse sampleGenreResponse() {
    return new TmdbGenreListResponse(List.of(new TmdbGenre(18, "드라마")));
  }
}
