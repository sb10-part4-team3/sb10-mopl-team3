package com.example.sb10_MoPl_team3.tmdb.batch.movie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.repository.ContentRepository;
import com.example.sb10_MoPl_team3.content.repository.ContentTagProjection;
import com.example.sb10_MoPl_team3.content.repository.ContentTagRepository;
import com.example.sb10_MoPl_team3.tmdb.cache.TmdbGenreCache;
import com.example.sb10_MoPl_team3.tmdb.client.TmdbApiClient;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbGenreListResponse;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbGenreListResponse.TmdbGenre;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbMoviePopularResponse;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbMoviePopularResponse.TmdbMovieResult;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import java.util.stream.LongStream;
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
class TmdbMovieSyncJobTest {

  private static final List<String> ALL_TEST_EXTERNAL_IDS = List.of(
      "MOVIE-9001", "MOVIE-9002",
      "MOVIE-100", "MOVIE-200", "MOVIE-300",
      "MOVIE-6001"
  );

  @Autowired
  private JobLauncher jobLauncher;

  @Autowired
  @Qualifier("tmdbMovieSyncJob")
  private Job tmdbMovieSyncJob;

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
    ReflectionTestUtils.setField(tmdbGenreCache, "movieGenres", null);
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
  void tmdbMovieSyncJob_실행하면_COMPLETED_상태로_종료되고_처리_건수가_일치한다() throws Exception {
    given(tmdbApiClient.getPopularMovies(1))
        .willReturn(pageResponse(1, 1, movie(9001), movie(9002)));
    given(tmdbApiClient.getMovieGenres()).willReturn(sampleGenreResponse());

    JobExecution execution = jobLauncher.run(tmdbMovieSyncJob, uniqueParams());

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

    StepExecution stepExecution = execution.getStepExecutions().iterator().next();
    assertThat(stepExecution.getReadCount()).isEqualTo(2);
    assertThat(stepExecution.getWriteCount()).isEqualTo(2);

    assertThat(contentRepository.findByExternalIdAndSource("MOVIE-9001", "TMDB")).isPresent();
    assertThat(contentRepository.findByExternalIdAndSource("MOVIE-9002", "TMDB")).isPresent();
  }

  @Test
  @DisplayName("이미 존재하는 콘텐츠는 갱신되고, 소프트 삭제된 콘텐츠는 건너뛰고, 신규 콘텐츠는 생성된다")
  void tmdbMovieSyncJob_기존_콘텐츠는_갱신되고_삭제된_콘텐츠는_건너뛰고_신규는_생성된다() throws Exception {
    // given: 이미 존재하는(삭제되지 않은) 콘텐츠 - 갱신 대상
    contentRepository.save(Content.builder()
        .type(ContentType.MOVIE)
        .title("갱신 전 제목")
        .description("갱신 전 설명")
        .thumbnailUrl("https://before.jpg")
        .externalId("MOVIE-100")
        .source("TMDB")
        .build());

    // given: 소프트 삭제된 콘텐츠 - 건너뛰기 대상
    Content deletedContent = contentRepository.save(Content.builder()
        .type(ContentType.MOVIE)
        .title("원본 제목")
        .description("원본 설명")
        .thumbnailUrl("https://old.jpg")
        .externalId("MOVIE-200")
        .source("TMDB")
        .build());
    UUID deletedContentId = deletedContent.getId();
    contentRepository.delete(deletedContent);
    contentRepository.flush();

    given(tmdbApiClient.getPopularMovies(1)).willReturn(pageResponse(1, 1,
        movie(100), movie(200), movie(300)));
    given(tmdbApiClient.getMovieGenres()).willReturn(sampleGenreResponse());

    JobExecution execution = jobLauncher.run(tmdbMovieSyncJob, uniqueParams());

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

    // then: 기존 콘텐츠는 갱신된다
    Content updated = contentRepository.findByExternalIdAndSource("MOVIE-100", "TMDB").orElseThrow();
    assertThat(updated.getTitle()).isEqualTo("제목100");

    // then: 소프트 삭제된 콘텐츠는 갱신되지 않고 그대로 유지된다
    Object[] deletedRow = (Object[]) entityManager.createNativeQuery(
            "SELECT title, deleted_at FROM contents WHERE external_id = :externalId")
        .setParameter("externalId", "MOVIE-200")
        .getSingleResult();
    assertThat(deletedRow[0]).isEqualTo("원본 제목");
    assertThat(deletedRow[1]).isNotNull();
    assertThat(contentTagRepository.findTagsByContentIds(List.of(deletedContentId))).isEmpty();

    // then: 신규 콘텐츠는 생성된다
    Content created = contentRepository.findByExternalIdAndSource("MOVIE-300", "TMDB").orElseThrow();
    assertThat(created.getTitle()).isEqualTo("제목300");
    List<ContentTagProjection> createdTags =
        contentTagRepository.findTagsByContentIds(List.of(created.getId()));
    assertThat(createdTags).extracting(ContentTagProjection::tagName).contains("액션");
  }

  @Test
  @DisplayName("Job이 실패한 후 같은 JobParameters로 재시작하면 이어서 실행되어 최종적으로 COMPLETED 된다")
  void tmdbMovieSyncJob_실패후_재시작하면_COMPLETED_된다() throws Exception {
    // given: 1페이지(청크 크기와 같은 20건)는 정상 응답, 2페이지는 첫 실행에서만 실패
    TmdbMovieResult[] firstPageMovies = LongStream.rangeClosed(5001, 5020)
        .mapToObj(this::movie)
        .toArray(TmdbMovieResult[]::new);
    given(tmdbApiClient.getPopularMovies(1))
        .willReturn(pageResponse(1, 2, firstPageMovies));
    given(tmdbApiClient.getPopularMovies(2))
        .willThrow(new RuntimeException("일시적인 원인불명 오류"))
        .willReturn(pageResponse(2, 2, movie(6001)));
    given(tmdbApiClient.getMovieGenres()).willReturn(sampleGenreResponse());

    JobParameters params = uniqueParams();

    // when: 첫 실행은 2페이지 조회 중 예외로 실패한다
    JobExecution firstExecution = jobLauncher.run(tmdbMovieSyncJob, params);
    assertThat(firstExecution.getStatus()).isEqualTo(BatchStatus.FAILED);

    // then: 첫 청크(20건)는 이미 커밋되어 있다
    for (long id = 5001; id <= 5020; id++) {
      assertThat(contentRepository.findByExternalIdAndSource("MOVIE-" + id, "TMDB")).isPresent();
    }
    assertThat(contentRepository.findByExternalIdAndSource("MOVIE-6001", "TMDB")).isEmpty();

    // when: 동일한 JobParameters로 재시작한다
    JobExecution restartExecution = jobLauncher.run(tmdbMovieSyncJob, params);

    // then: 재시작된 실행은 COMPLETED 되고, 나머지 데이터까지 모두 반영된다
    assertThat(restartExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(contentRepository.findByExternalIdAndSource("MOVIE-6001", "TMDB")).isPresent();

    then(tmdbApiClient).should(times(2)).getPopularMovies(2);
  }

  private JobParameters uniqueParams() {
    return new JobParametersBuilder()
        .addLong("time", System.nanoTime())
        .toJobParameters();
  }

  private TmdbMoviePopularResponse pageResponse(int page, int totalPages, TmdbMovieResult... results) {
    return new TmdbMoviePopularResponse(page, List.of(results), totalPages, results.length);
  }

  private TmdbMovieResult movie(long id) {
    return new TmdbMovieResult(
        id, "제목" + id, "Title" + id, "개요" + id, "/poster.jpg", "/backdrop.jpg",
        "2024-01-01", 10.0, 7.5, 100, List.of(28), false);
  }

  private TmdbGenreListResponse sampleGenreResponse() {
    return new TmdbGenreListResponse(List.of(new TmdbGenre(28, "액션")));
  }
}
