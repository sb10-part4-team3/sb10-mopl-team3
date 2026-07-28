package com.example.sb10_MoPl_team3.tmdb.batch.tv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.sb10_MoPl_team3.tmdb.client.TmdbApiClient;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbTvPopularResponse;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbTvPopularResponse.TmdbTvResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TmdbTvItemReaderTest {

  @Mock
  private TmdbApiClient tmdbApiClient;

  @Test
  void read_여러_페이지의_결과를_순서대로_반환하고_마지막에는_null을_반환한다() {
    TmdbTvItemReader reader = new TmdbTvItemReader(tmdbApiClient);

    given(tmdbApiClient.getPopularTvs(1)).willReturn(pageResponse(1, 2, tv(1), tv(2)));
    given(tmdbApiClient.getPopularTvs(2)).willReturn(pageResponse(2, 2, tv(3)));

    assertThat(reader.read().id()).isEqualTo(1L);
    assertThat(reader.read().id()).isEqualTo(2L);
    assertThat(reader.read().id()).isEqualTo(3L);
    assertThat(reader.read()).isNull();

    then(tmdbApiClient).should().getPopularTvs(1);
    then(tmdbApiClient).should().getPopularTvs(2);
  }

  @Test
  void read_빈_페이지는_건너뛰고_다음_페이지를_조회한다() {
    TmdbTvItemReader reader = new TmdbTvItemReader(tmdbApiClient);

    given(tmdbApiClient.getPopularTvs(1)).willReturn(pageResponse(1, 3));
    given(tmdbApiClient.getPopularTvs(2)).willReturn(pageResponse(2, 3));
    given(tmdbApiClient.getPopularTvs(3)).willReturn(pageResponse(3, 3, tv(1)));

    assertThat(reader.read().id()).isEqualTo(1L);
    assertThat(reader.read()).isNull();

    then(tmdbApiClient).should().getPopularTvs(1);
    then(tmdbApiClient).should().getPopularTvs(2);
    then(tmdbApiClient).should().getPopularTvs(3);
  }

  @Test
  void read_maxPages가_설정되면_그_이후_페이지는_조회하지_않는다() {
    TmdbTvItemReader reader = new TmdbTvItemReader(tmdbApiClient);
    ReflectionTestUtils.setField(reader, "maxPages", 1);

    given(tmdbApiClient.getPopularTvs(1)).willReturn(pageResponse(1, 5, tv(1)));

    assertThat(reader.read().id()).isEqualTo(1L);
    assertThat(reader.read()).isNull();

    then(tmdbApiClient).should().getPopularTvs(1);
    then(tmdbApiClient).should(never()).getPopularTvs(2);
  }

  @Test
  void read_TMDB_최대_페이지인_500을_초과하면_totalPages와_무관하게_더_이상_요청하지_않는다() {
    TmdbTvItemReader reader = new TmdbTvItemReader(tmdbApiClient);
    ReflectionTestUtils.setField(reader, "currentPage", 500);
    ReflectionTestUtils.setField(reader, "totalPages", 100000);

    given(tmdbApiClient.getPopularTvs(500)).willReturn(pageResponse(500, 100000, tv(1)));

    assertThat(reader.read().id()).isEqualTo(1L);
    assertThat(reader.read()).isNull();

    then(tmdbApiClient).should().getPopularTvs(500);
    then(tmdbApiClient).should(never()).getPopularTvs(501);
  }

  private TmdbTvPopularResponse pageResponse(int page, int totalPages, TmdbTvResult... results) {
    return new TmdbTvPopularResponse(page, List.of(results), totalPages, results.length);
  }

  private TmdbTvResult tv(long id) {
    return new TmdbTvResult(
        id, "제목" + id, "Title" + id, "개요", "/poster.jpg", "/backdrop.jpg",
        "2024-01-01", 10.0, 7.5, 100, List.of(18), List.of("US"));
  }
}
