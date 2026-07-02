package com.example.sb10_MoPl_team3.tmdb.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.repository.ContentRepository;
import com.example.sb10_MoPl_team3.content.repository.ContentTagProjection;
import com.example.sb10_MoPl_team3.content.repository.ContentTagRepository;
import com.example.sb10_MoPl_team3.tmdb.TmdbConstants;
import com.example.sb10_MoPl_team3.tmdb.client.TmdbApiClient;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbGenreListResponse;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbGenreListResponse.TmdbGenre;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbMoviePopularResponse;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbMoviePopularResponse.TmdbMovieResult;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbTvPopularResponse;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbTvPopularResponse.TmdbTvResult;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TmdbContentSyncServiceSoftDeleteIntegrationTest {

  private static final String DELETED_EXTERNAL_ID = TmdbConstants.externalId("MOVIE", 100L);
  private static final String UPDATED_EXTERNAL_ID = TmdbConstants.externalId("MOVIE", 200L);
  private static final String NEW_EXTERNAL_ID = TmdbConstants.externalId("MOVIE", 300L);

  private static final String DELETED_TV_EXTERNAL_ID = TmdbConstants.externalId("TV", 100L);
  private static final String UPDATED_TV_EXTERNAL_ID = TmdbConstants.externalId("TV", 200L);
  private static final String NEW_TV_EXTERNAL_ID = TmdbConstants.externalId("TV", 300L);

  @Autowired
  private TmdbContentSyncService tmdbContentSyncService;

  @Autowired
  private ContentRepository contentRepository;

  @Autowired
  private ContentTagRepository contentTagRepository;

  @Autowired
  private EntityManager entityManager;

  @MockitoBean
  private TmdbApiClient tmdbApiClient;

  @Test
  @DisplayName("소프트 삭제된 콘텐츠는 갱신을 건너뛰고, 나머지 콘텐츠는 정상적으로 동기화된다")
  void upsert_소프트삭제된_콘텐츠는_건너뛰고_나머지는_동기화된다() {
    // given: 소프트 삭제된 콘텐츠
    Content deletedContent = contentRepository.save(Content.builder()
        .type(ContentType.MOVIE)
        .title("원본 제목")
        .description("원본 설명")
        .thumbnailUrl("https://image.tmdb.org/t/p/w500/old.jpg")
        .externalId(DELETED_EXTERNAL_ID)
        .source(TmdbConstants.SOURCE_TMDB)
        .build());
    UUID deletedContentId = deletedContent.getId();
    contentRepository.delete(deletedContent);
    contentRepository.flush();

    // given: 이미 존재하는(삭제되지 않은) 콘텐츠 - 갱신 대상
    contentRepository.save(Content.builder()
        .type(ContentType.MOVIE)
        .title("갱신 전 제목")
        .description("갱신 전 설명")
        .thumbnailUrl("https://image.tmdb.org/t/p/w500/before.jpg")
        .externalId(UPDATED_EXTERNAL_ID)
        .source(TmdbConstants.SOURCE_TMDB)
        .build());

    // given: upsert가 REQUIRES_NEW로 별도 트랜잭션에서 동작하므로, setup 데이터를 커밋해야 보인다
    TestTransaction.flagForCommit();
    TestTransaction.end();
    TestTransaction.start();
    TestTransaction.flagForCommit();

    given(tmdbApiClient.getMovieGenres())
        .willReturn(new TmdbGenreListResponse(List.of(new TmdbGenre(28, "액션"))));

    TmdbMovieResult deletedResult = new TmdbMovieResult(
        100L, "TMDB가 보낸 새 제목", "Original Title", "TMDB가 보낸 새 설명", "/new.jpg", "/backdrop.jpg",
        "2026-01-01", 10.0, 8.0, 100, List.of(28), false);
    TmdbMovieResult updatedResult = new TmdbMovieResult(
        200L, "갱신 후 제목", "Updated Title", "갱신 후 설명", "/updated.jpg", "/backdrop2.jpg",
        "2026-02-01", 20.0, 9.0, 200, List.of(28), false);
    TmdbMovieResult newResult = new TmdbMovieResult(
        300L, "새 영화", "New Movie", "새 영화 설명", "/new3.jpg", "/backdrop3.jpg",
        "2026-03-01", 30.0, 7.0, 300, List.of(28), false);

    given(tmdbApiClient.getPopularMovies(1))
        .willReturn(new TmdbMoviePopularResponse(1, List.of(deletedResult, updatedResult, newResult), 1, 3));

    // when
    tmdbContentSyncService.syncPopularMovies(1);
    entityManager.flush();
    entityManager.clear();

    // then: 소프트 삭제된 콘텐츠는 갱신되지 않는다 (title/description 원본 유지, deleted_at 유지)
    Object[] deletedRow = (Object[]) entityManager.createNativeQuery(
            "SELECT title, description, deleted_at FROM contents WHERE external_id = :externalId AND source = :source")
        .setParameter("externalId", DELETED_EXTERNAL_ID)
        .setParameter("source", TmdbConstants.SOURCE_TMDB)
        .getSingleResult();

    assertThat(deletedRow[0]).isEqualTo("원본 제목");
    assertThat(deletedRow[1]).isEqualTo("원본 설명");
    assertThat(deletedRow[2]).isNotNull();

    List<ContentTagProjection> deletedContentTags =
        contentTagRepository.findTagsByContentIds(List.of(deletedContentId));
    assertThat(deletedContentTags).isEmpty();

    // then: 기존 콘텐츠는 정상적으로 갱신된다
    Content updated = contentRepository.findByExternalIdAndSource(UPDATED_EXTERNAL_ID, TmdbConstants.SOURCE_TMDB)
        .orElseThrow();
    assertThat(updated.getTitle()).isEqualTo("갱신 후 제목");
    assertThat(updated.getDescription()).isEqualTo("갱신 후 설명");

    List<ContentTagProjection> updatedContentTags =
        contentTagRepository.findTagsByContentIds(List.of(updated.getId()));
    assertThat(updatedContentTags).extracting(ContentTagProjection::tagName).contains("액션");

    // then: 새 콘텐츠는 정상적으로 저장된다
    Content created = contentRepository.findByExternalIdAndSource(NEW_EXTERNAL_ID, TmdbConstants.SOURCE_TMDB)
        .orElseThrow();
    assertThat(created.getTitle()).isEqualTo("새 영화");
    assertThat(created.getDescription()).isEqualTo("새 영화 설명");

    List<ContentTagProjection> createdContentTags =
        contentTagRepository.findTagsByContentIds(List.of(created.getId()));
    assertThat(createdContentTags).extracting(ContentTagProjection::tagName).contains("액션");

    // cleanup: REQUIRES_NEW로 커밋된 데이터는 테스트 트랜잭션 롤백으로 지워지지 않으므로 직접 삭제한다
    cleanUpContents(DELETED_EXTERNAL_ID, UPDATED_EXTERNAL_ID, NEW_EXTERNAL_ID);
  }

  @Test
  @DisplayName("소프트 삭제된 TV 콘텐츠는 갱신을 건너뛰고, 나머지 TV 콘텐츠는 정상적으로 동기화된다")
  void upsertTv_소프트삭제된_콘텐츠는_건너뛰고_나머지는_동기화된다() {
    // given: 소프트 삭제된 콘텐츠
    Content deletedContent = contentRepository.save(Content.builder()
        .type(ContentType.TV_SERIES)
        .title("원본 제목")
        .description("원본 설명")
        .thumbnailUrl("https://image.tmdb.org/t/p/w500/old.jpg")
        .externalId(DELETED_TV_EXTERNAL_ID)
        .source(TmdbConstants.SOURCE_TMDB)
        .build());
    UUID deletedContentId = deletedContent.getId();
    contentRepository.delete(deletedContent);
    contentRepository.flush();

    // given: 이미 존재하는(삭제되지 않은) 콘텐츠 - 갱신 대상
    contentRepository.save(Content.builder()
        .type(ContentType.TV_SERIES)
        .title("갱신 전 제목")
        .description("갱신 전 설명")
        .thumbnailUrl("https://image.tmdb.org/t/p/w500/before.jpg")
        .externalId(UPDATED_TV_EXTERNAL_ID)
        .source(TmdbConstants.SOURCE_TMDB)
        .build());

    // given: upsert가 REQUIRES_NEW로 별도 트랜잭션에서 동작하므로, setup 데이터를 커밋해야 보인다
    TestTransaction.flagForCommit();
    TestTransaction.end();
    TestTransaction.start();
    TestTransaction.flagForCommit();

    given(tmdbApiClient.getTvGenres())
        .willReturn(new TmdbGenreListResponse(List.of(new TmdbGenre(18, "드라마"))));

    TmdbTvResult deletedResult = new TmdbTvResult(
        100L, "TMDB가 보낸 새 제목", "Original Name", "TMDB가 보낸 새 설명", "/new.jpg", "/backdrop.jpg",
        "2026-01-01", 10.0, 8.0, 100, List.of(18), List.of("KR"));
    TmdbTvResult updatedResult = new TmdbTvResult(
        200L, "갱신 후 제목", "Updated Name", "갱신 후 설명", "/updated.jpg", "/backdrop2.jpg",
        "2026-02-01", 20.0, 9.0, 200, List.of(18), List.of("KR"));
    TmdbTvResult newResult = new TmdbTvResult(
        300L, "새 드라마", "New Drama", "새 드라마 설명", "/new3.jpg", "/backdrop3.jpg",
        "2026-03-01", 30.0, 7.0, 300, List.of(18), List.of("KR"));

    given(tmdbApiClient.getPopularTvs(1))
        .willReturn(new TmdbTvPopularResponse(1, List.of(deletedResult, updatedResult, newResult), 1, 3));

    // when
    tmdbContentSyncService.syncPopularTvs(1);
    entityManager.flush();
    entityManager.clear();

    // then: 소프트 삭제된 콘텐츠는 갱신되지 않는다 (title/description 원본 유지, deleted_at 유지)
    Object[] deletedRow = (Object[]) entityManager.createNativeQuery(
            "SELECT title, description, deleted_at FROM contents WHERE external_id = :externalId AND source = :source")
        .setParameter("externalId", DELETED_TV_EXTERNAL_ID)
        .setParameter("source", TmdbConstants.SOURCE_TMDB)
        .getSingleResult();

    assertThat(deletedRow[0]).isEqualTo("원본 제목");
    assertThat(deletedRow[1]).isEqualTo("원본 설명");
    assertThat(deletedRow[2]).isNotNull();

    List<ContentTagProjection> deletedContentTags =
        contentTagRepository.findTagsByContentIds(List.of(deletedContentId));
    assertThat(deletedContentTags).isEmpty();

    // then: 기존 콘텐츠는 정상적으로 갱신된다
    Content updated = contentRepository.findByExternalIdAndSource(UPDATED_TV_EXTERNAL_ID, TmdbConstants.SOURCE_TMDB)
        .orElseThrow();
    assertThat(updated.getTitle()).isEqualTo("갱신 후 제목");
    assertThat(updated.getDescription()).isEqualTo("갱신 후 설명");

    List<ContentTagProjection> updatedContentTags =
        contentTagRepository.findTagsByContentIds(List.of(updated.getId()));
    assertThat(updatedContentTags).extracting(ContentTagProjection::tagName).contains("드라마");

    // then: 새 콘텐츠는 정상적으로 저장된다
    Content created = contentRepository.findByExternalIdAndSource(NEW_TV_EXTERNAL_ID, TmdbConstants.SOURCE_TMDB)
        .orElseThrow();
    assertThat(created.getTitle()).isEqualTo("새 드라마");
    assertThat(created.getDescription()).isEqualTo("새 드라마 설명");

    List<ContentTagProjection> createdContentTags =
        contentTagRepository.findTagsByContentIds(List.of(created.getId()));
    assertThat(createdContentTags).extracting(ContentTagProjection::tagName).contains("드라마");

    // cleanup: REQUIRES_NEW로 커밋된 데이터는 테스트 트랜잭션 롤백으로 지워지지 않으므로 직접 삭제한다
    cleanUpContents(DELETED_TV_EXTERNAL_ID, UPDATED_TV_EXTERNAL_ID, NEW_TV_EXTERNAL_ID);
  }

  private void cleanUpContents(String... externalIds) {
    List<String> ids = List.of(externalIds);
    entityManager.createNativeQuery(
            "DELETE FROM content_tags WHERE content_id IN (SELECT id FROM contents WHERE external_id IN (:ids))")
        .setParameter("ids", ids)
        .executeUpdate();
    entityManager.createNativeQuery("DELETE FROM contents WHERE external_id IN (:ids)")
        .setParameter("ids", ids)
        .executeUpdate();
  }
}
