package com.example.sb10_MoPl_team3.tmdb.batch.movie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.sb10_MoPl_team3.tmdb.client.TmdbApiClient;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbMoviePopularResponse;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbMoviePopularResponse.TmdbMovieResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TmdbMovieItemReaderTest {

  @Mock
  private TmdbApiClient tmdbApiClient;

  @Test
  void read_여러_페이지의_결과를_순서대로_반환하고_마지막에는_null을_반환한다() {
    TmdbMovieItemReader reader = new TmdbMovieItemReader(tmdbApiClient);

    given(tmdbApiClient.getPopularMovies(1)).willReturn(pageResponse(1, 2, movie(1), movie(2)));
    given(tmdbApiClient.getPopularMovies(2)).willReturn(pageResponse(2, 2, movie(3)));

    assertThat(reader.read().id()).isEqualTo(1L);
    assertThat(reader.read().id()).isEqualTo(2L);
    assertThat(reader.read().id()).isEqualTo(3L);
    assertThat(reader.read()).isNull();

    then(tmdbApiClient).should().getPopularMovies(1);
    then(tmdbApiClient).should().getPopularMovies(2);
  }

  @Test
  void read_maxPages가_설정되면_그_이후_페이지는_조회하지_않는다() {
    TmdbMovieItemReader reader = new TmdbMovieItemReader(tmdbApiClient);
    ReflectionTestUtils.setField(reader, "maxPages", 1);

    given(tmdbApiClient.getPopularMovies(1)).willReturn(pageResponse(1, 5, movie(1)));

    assertThat(reader.read().id()).isEqualTo(1L);
    assertThat(reader.read()).isNull();

    then(tmdbApiClient).should().getPopularMovies(1);
    then(tmdbApiClient).should(never()).getPopularMovies(2);
  }

  private TmdbMoviePopularResponse pageResponse(int page, int totalPages, TmdbMovieResult... results) {
    return new TmdbMoviePopularResponse(page, List.of(results), totalPages, results.length);
  }

  private TmdbMovieResult movie(long id) {
    return new TmdbMovieResult(
        id, "제목" + id, "Title" + id, "개요", "/poster.jpg", "/backdrop.jpg",
        "2024-01-01", 10.0, 7.5, 100, List.of(28), false);
  }
}
