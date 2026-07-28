package com.example.sb10_MoPl_team3.tmdb.batch.tv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.tmdb.cache.TmdbGenreCache;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbTvPopularResponse.TmdbTvResult;
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
class TmdbTvPayloadProcessorTest {

  @Mock
  private TmdbGenreCache tmdbGenreCache;

  @Mock
  private TmdbContentMapper tmdbContentMapper;

  @InjectMocks
  private TmdbTvPayloadProcessor tmdbTvPayloadProcessor;

  @Test
  void process_TV_결과를_SyncPayload로_매핑한다() {
    TmdbTvResult result = new TmdbTvResult(
        1399L, "왕좌의 게임", "Game of Thrones", "설명입니다", "/poster.jpg", "/backdrop.jpg",
        "2011-04-17", 300.5, 8.4, 21000, List.of(18, 10765), List.of("US"));

    given(tmdbGenreCache.getTvGenres()).willReturn(Map.of(18, "드라마", 10765, "SF & 판타지"));
    Content mappedContent = Content.builder()
        .type(ContentType.TV_SERIES)
        .title("왕좌의 게임")
        .externalId("TV-1399")
        .source("TMDB")
        .build();
    given(tmdbContentMapper.toContent(result)).willReturn(mappedContent);

    SyncPayload payload = tmdbTvPayloadProcessor.process(result);

    assertThat(payload.externalId()).isEqualTo("TV-1399");
    assertThat(payload.title()).isEqualTo("왕좌의 게임");
    assertThat(payload.overview()).isEqualTo("설명입니다");
    assertThat(payload.posterPath()).isEqualTo("/poster.jpg");
    assertThat(payload.genreNames()).containsExactly("드라마", "SF & 판타지");
    assertThat(payload.newContentSupplier().get()).isSameAs(mappedContent);
  }

  @Test
  void process_장르맵에_없는_장르_id는_기타로_대체된다() {
    TmdbTvResult result = new TmdbTvResult(
        1L, "제목", "Title", "개요", "/poster.jpg", "/backdrop.jpg",
        "2024-01-01", 10.0, 7.5, 100, List.of(999), List.of("US"));

    given(tmdbGenreCache.getTvGenres()).willReturn(Map.of(18, "드라마"));

    SyncPayload payload = tmdbTvPayloadProcessor.process(result);

    assertThat(payload.genreNames()).containsExactly("기타");
  }
}
