package com.example.sb10_MoPl_team3.tmdb.batch.tv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.repository.ContentRepository;
import com.example.sb10_MoPl_team3.content.repository.ContentTagProjection;
import com.example.sb10_MoPl_team3.content.repository.ContentTagRepository;
import com.example.sb10_MoPl_team3.tmdb.cache.TmdbGenreCache;
import com.example.sb10_MoPl_team3.tmdb.client.TmdbApiClient;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbGenreListResponse;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbGenreListResponse.TmdbGenre;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbTvPopularResponse;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbTvPopularResponse.TmdbTvResult;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
class TmdbTvSyncJobTest {

  private static final List<String> ALL_TEST_EXTERNAL_IDS = List.of(
      "TV-9001", "TV-9002",
      "TV-100", "TV-200", "TV-300"
  );

  @Autowired
  private JobLauncher jobLauncher;

  @Autowired
  @Qualifier("tmdbTvSyncJob")
  private Job tmdbTvSyncJob;

  @Autowired
  private TmdbGenreCache tmdbGenreCache;

  @Autowired
  private ContentRepository contentRepository;

  @Autowired
  private ContentTagRepository contentTagRepository;

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private PlatformTransactionManager transactionManager;

  @MockitoBean
  private TmdbApiClient tmdbApiClient;

  @BeforeEach
  void resetGenreCache() {
    // TmdbGenreCache는 싱글턴으로 한 번 채워지면 계속 캐시되므로, 테스트마다 목 응답이 반영되도록 초기화한다
    ReflectionTestUtils.setField(tmdbGenreCache, "tvGenres", null);
  }

  @AfterEach
  void cleanUp() {
    new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
      entityManager.createNativeQuery(
              "DELETE FROM content_tags WHERE content_id IN (SELECT id FROM contents WHERE external_id IN (:ids))")
          .setParameter("ids", ALL_TEST_EXTERNAL_IDS)
          .executeUpdate();

      entityManager.createNativeQuery(
              "DELETE FROM content_stats WHERE content_id IN (SELECT id FROM contents WHERE external_id IN (:ids))")
          .setParameter("ids", ALL_TEST_EXTERNAL_IDS)
          .executeUpdate();

      entityManager.createNativeQuery("DELETE FROM contents WHERE external_id IN (:ids)")
          .setParameter("ids", ALL_TEST_EXTERNAL_IDS)
          .executeUpdate();
    });
  }

  @Test
  @DisplayName("Job을 실행하면 COMPLETED 상태로 종료되고, 읽고 쓴 건수가 응답 결과 수와 일치한다")
  void tmdbTvSyncJob_실행하면_COMPLETED_상태로_종료되고_처리_건수가_일치한다() throws Exception {
    given(tmdbApiClient.getPopularTvs(1)).willReturn(pageResponse(1, 1, tv(9001), tv(9002)));
    given(tmdbApiClient.getTvGenres()).willReturn(sampleGenreResponse());

    JobExecution execution = jobLauncher.run(tmdbTvSyncJob, uniqueParams());

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

    StepExecution stepExecution = execution.getStepExecutions().iterator().next();
    assertThat(stepExecution.getReadCount()).isEqualTo(2);
    assertThat(stepExecution.getWriteCount()).isEqualTo(2);

    assertThat(contentRepository.findByExternalIdAndSource("TV-9001", "TMDB")).isPresent();
    assertThat(contentRepository.findByExternalIdAndSource("TV-9002", "TMDB")).isPresent();
  }

  @Test
  @DisplayName("이미 존재하는 콘텐츠는 갱신되고, 소프트 삭제된 콘텐츠는 건너뛰고, 신규 콘텐츠는 생성된다")
  void tmdbTvSyncJob_기존_콘텐츠는_갱신되고_삭제된_콘텐츠는_건너뛰고_신규는_생성된다() throws Exception {
    contentRepository.save(Content.builder()
        .type(ContentType.TV_SERIES)
        .title("갱신 전 제목")
        .description("갱신 전 설명")
        .thumbnailUrl("https://before.jpg")
        .externalId("TV-100")
        .source("TMDB")
        .build());

    Content deletedContent = contentRepository.save(Content.builder()
        .type(ContentType.TV_SERIES)
        .title("원본 제목")
        .description("원본 설명")
        .thumbnailUrl("https://old.jpg")
        .externalId("TV-200")
        .source("TMDB")
        .build());
    UUID deletedContentId = deletedContent.getId();
    contentRepository.delete(deletedContent);
    contentRepository.flush();

    given(tmdbApiClient.getPopularTvs(1)).willReturn(pageResponse(1, 1,
        tv(100), tv(200), tv(300)));
    given(tmdbApiClient.getTvGenres()).willReturn(sampleGenreResponse());

    JobExecution execution = jobLauncher.run(tmdbTvSyncJob, uniqueParams());

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

    Content updated = contentRepository.findByExternalIdAndSource("TV-100", "TMDB").orElseThrow();
    assertThat(updated.getTitle()).isEqualTo("제목100");

    Object[] deletedRow = (Object[]) entityManager.createNativeQuery(
            "SELECT title, deleted_at FROM contents WHERE external_id = :externalId")
        .setParameter("externalId", "TV-200")
        .getSingleResult();
    assertThat(deletedRow[0]).isEqualTo("원본 제목");
    assertThat(deletedRow[1]).isNotNull();
    assertThat(contentTagRepository.findTagsByContentIds(List.of(deletedContentId))).isEmpty();

    Content created = contentRepository.findByExternalIdAndSource("TV-300", "TMDB").orElseThrow();
    assertThat(created.getTitle()).isEqualTo("제목300");
    List<ContentTagProjection> createdTags =
        contentTagRepository.findTagsByContentIds(List.of(created.getId()));
    assertThat(createdTags).extracting(ContentTagProjection::tagName).contains("드라마");
  }

  private JobParameters uniqueParams() {
    return new JobParametersBuilder()
        .addLong("time", System.nanoTime())
        .toJobParameters();
  }

  private TmdbTvPopularResponse pageResponse(int page, int totalPages, TmdbTvResult... results) {
    return new TmdbTvPopularResponse(page, List.of(results), totalPages, results.length);
  }

  private TmdbTvResult tv(long id) {
    return new TmdbTvResult(
        id, "제목" + id, "Title" + id, "개요" + id, "/poster.jpg", "/backdrop.jpg",
        "2024-01-01", 10.0, 7.5, 100, List.of(18), List.of("US"));
  }

  private TmdbGenreListResponse sampleGenreResponse() {
    return new TmdbGenreListResponse(List.of(new TmdbGenre(18, "드라마")));
  }
}
