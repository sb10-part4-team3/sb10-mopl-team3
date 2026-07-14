package com.example.sb10_MoPl_team3.tmdb.batch.movie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.tmdb.cache.TmdbGenreCache;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbMoviePopularResponse.TmdbMovieResult;
import com.example.sb10_MoPl_team3.tmdb.mapper.TmdbContentMapper;
import com.example.sb10_MoPl_team3.tmdb.service.SyncPayload;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TmdbMoviePayloadProcessorTest {

  @Mock
  private TmdbGenreCache tmdbGenreCache;

  @Mock
  private TmdbContentMapper tmdbContentMapper;

  @InjectMocks
  private TmdbMoviePayloadProcessor tmdbMoviePayloadProcessor;

  @Test
  void process_영화_결과를_SyncPayload로_매핑한다() {
    TmdbMovieResult result = new TmdbMovieResult(
        550L, "파이트 클럽", "Fight Club", "설명입니다", "/poster.jpg", "/backdrop.jpg",
        "1999-10-15", 61.4, 8.4, 26280, List.of(18, 53), false);

    given(tmdbGenreCache.getMovieGenres()).willReturn(Map.of(18, "드라마", 53, "스릴러"));
    Content mappedContent = Content.builder()
        .type(ContentType.MOVIE)
        .title("파이트 클럽")
        .externalId("MOVIE-550")
        .source("TMDB")
        .build();
    given(tmdbContentMapper.toContent(result)).willReturn(mappedContent);

    SyncPayload payload = tmdbMoviePayloadProcessor.process(result);

    assertThat(payload.externalId()).isEqualTo("MOVIE-550");
    assertThat(payload.title()).isEqualTo("파이트 클럽");
    assertThat(payload.overview()).isEqualTo("설명입니다");
    assertThat(payload.posterPath()).isEqualTo("/poster.jpg");
    assertThat(payload.genreNames()).containsExactly("드라마", "스릴러");
    assertThat(payload.newContentSupplier().get()).isSameAs(mappedContent);
  }

  @Test
  void process_장르맵에_없는_장르_id는_기타로_대체된다() {
    TmdbMovieResult result = new TmdbMovieResult(
        1L, "제목", "Title", "개요", "/poster.jpg", "/backdrop.jpg",
        "2024-01-01", 10.0, 7.5, 100, List.of(999), false);

    given(tmdbGenreCache.getMovieGenres()).willReturn(Map.of(28, "액션"));

    SyncPayload payload = tmdbMoviePayloadProcessor.process(result);

    assertThat(payload.genreNames()).containsExactly("기타");
  }
}
