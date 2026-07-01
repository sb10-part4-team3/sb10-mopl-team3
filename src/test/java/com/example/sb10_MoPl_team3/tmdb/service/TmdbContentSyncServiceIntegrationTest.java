package com.example.sb10_MoPl_team3.tmdb.service;

import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.entity.ContentTag;
import com.example.sb10_MoPl_team3.content.repository.ContentRepository;
import com.example.sb10_MoPl_team3.content.repository.ContentTagProjection;
import com.example.sb10_MoPl_team3.content.repository.ContentTagRepository;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest
@Transactional
@Tag("external")
class TmdbContentSyncServiceIntegrationTest {

  @Autowired
  private TmdbContentSyncService tmdbContentSyncService;

  @Autowired
  private ContentRepository contentRepository;

  @Autowired
  private ContentTagRepository contentTagRepository;

  @Value("${tmdb.access-token:}")
  private String accessToken;

  @Test
  @DisplayName("인기 영화를 동기화하면 Content 테이블에 저장된다")
  void syncPopularMovies_저장된다() {
    assumeTrue(!accessToken.isBlank(), "TMDB_ACCESS_TOKEN 미설정 — 테스트를 건너뜁니다.");

    tmdbContentSyncService.syncPopularMovies(1);

    List<Content> saved = contentRepository.findAll().stream()
        .filter(content -> "TMDB".equals(content.getSource()))
        .filter(content -> content.getExternalId().startsWith("MOVIE-"))
        .toList();

    assertThat(saved).isNotEmpty();
    assertThat(saved).allSatisfy(content -> {
      assertThat(content.getTitle()).isNotBlank();
      assertThat(content.getSource()).isEqualTo("TMDB");
    });

    // 태그 검증은 별도 블록으로 — 첫 번째 영화 하나만 대표로 확인
    UUID firstMovieId = saved.get(0).getId();
    List<ContentTagProjection> tags = contentTagRepository.findTagsByContentIds(List.of(firstMovieId));
    assertThat(tags).isNotEmpty();
  }

  @Test
  @DisplayName("인기 TV를 동기화하면 Content 테이블에 저장된다")
  void syncPopularTvs_저장된다() {
    assumeTrue(!accessToken.isBlank(), "TMDB_ACCESS_TOKEN 미설정 — 테스트를 건너뜁니다.");

    tmdbContentSyncService.syncPopularTvs(1);

    List<Content> saved = contentRepository.findAll().stream()
        .filter(content -> "TMDB".equals(content.getSource()))
        .filter(content -> content.getExternalId().startsWith("TV-"))
        .toList();

    assertThat(saved).isNotEmpty();
    assertThat(saved).allSatisfy(content -> {
      assertThat(content.getTitle()).isNotBlank();
      assertThat(content.getSource()).isEqualTo("TMDB");
    });

    UUID firstTvId = saved.get(0).getId();
    List<ContentTagProjection> tags = contentTagRepository.findTagsByContentIds(List.of(firstTvId));
    assertThat(tags).isNotEmpty();
  }
}