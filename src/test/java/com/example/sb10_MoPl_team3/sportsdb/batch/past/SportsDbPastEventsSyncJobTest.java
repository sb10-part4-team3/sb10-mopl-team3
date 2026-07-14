package com.example.sb10_MoPl_team3.sportsdb.batch.past;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.repository.ContentRepository;
import com.example.sb10_MoPl_team3.content.repository.ContentTagProjection;
import com.example.sb10_MoPl_team3.content.repository.ContentTagRepository;
import com.example.sb10_MoPl_team3.sportsdb.SportsDbConstants;
import com.example.sb10_MoPl_team3.sportsdb.client.SportsDbApiClient;
import com.example.sb10_MoPl_team3.sportsdb.config.SportsDbProperties;
import com.example.sb10_MoPl_team3.sportsdb.dto.SportsDbEventsResponse;
import com.example.sb10_MoPl_team3.sportsdb.dto.SportsDbEventsResponse.SportsDbEvent;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
class SportsDbPastEventsSyncJobTest {

  private static final String LEAGUE_ID = "4328";

  private static final List<String> ALL_TEST_EXTERNAL_IDS = List.of(
      SportsDbConstants.externalId("1"),
      SportsDbConstants.externalId("10"), SportsDbConstants.externalId("11"),
      SportsDbConstants.externalId("100"), SportsDbConstants.externalId("200"),
      SportsDbConstants.externalId("300")
  );

  @Autowired
  private JobLauncher jobLauncher;

  @Autowired
  @Qualifier("sportsDbPastEventsSyncJob")
  private Job sportsDbPastEventsSyncJob;

  @Autowired
  private ContentRepository contentRepository;

  @Autowired
  private ContentTagRepository contentTagRepository;

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private PlatformTransactionManager transactionManager;

  @MockitoBean
  private SportsDbApiClient sportsDbApiClient;

  @MockitoBean
  private SportsDbProperties sportsDbProperties;

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
  void sportsDbPastEventsSyncJob_실행하면_COMPLETED_상태로_종료된다() throws Exception {
    // given
    given(sportsDbProperties.getTargetLeagueIds()).willReturn(List.of(LEAGUE_ID));
    given(sportsDbApiClient.getPastEventsByLeague(LEAGUE_ID)).willReturn(sampleEventsResponse());

    JobParameters params = uniqueParams();

    // when
    JobExecution execution = jobLauncher.run(sportsDbPastEventsSyncJob, params);

    // then
    assertThat(execution.getStatus().toString()).isEqualTo("COMPLETED");
  }

  @Test
  @DisplayName("Job을 실행하면 응답받은 경기 수만큼 콘텐츠가 저장된다")
  void sportsDbPastEventsSyncJob_실행하면_이벤트_건수만큼_콘텐츠가_저장된다() throws Exception {
    given(sportsDbProperties.getTargetLeagueIds()).willReturn(List.of(LEAGUE_ID));
    given(sportsDbApiClient.getPastEventsByLeague(LEAGUE_ID)).willReturn(
        new SportsDbEventsResponse(List.of(event("10"), event("11"))));

    JobExecution execution = jobLauncher.run(sportsDbPastEventsSyncJob, uniqueParams());

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(contentRepository.findByExternalIdAndSource(
        SportsDbConstants.externalId("10"), SportsDbConstants.SOURCE_SPORTS_DB)).isPresent();
    assertThat(contentRepository.findByExternalIdAndSource(
        SportsDbConstants.externalId("11"), SportsDbConstants.SOURCE_SPORTS_DB)).isPresent();
  }

  @Test
  @DisplayName("이미 존재하는 경기는 갱신되고, 소프트 삭제된 경기는 건너뛰고, 신규 경기는 생성된다")
  void sportsDbPastEventsSyncJob_기존_경기는_갱신되고_삭제된_경기는_건너뛰고_신규는_생성된다() throws Exception {
    contentRepository.save(Content.builder()
        .type(ContentType.SPORT)
        .title("갱신 전 경기명")
        .description("갱신 전 설명")
        .thumbnailUrl("https://before.jpg")
        .externalId(SportsDbConstants.externalId("100"))
        .source(SportsDbConstants.SOURCE_SPORTS_DB)
        .build());

    Content deletedContent = contentRepository.save(Content.builder()
        .type(ContentType.SPORT)
        .title("원본 경기명")
        .description("원본 설명")
        .thumbnailUrl("https://old.jpg")
        .externalId(SportsDbConstants.externalId("200"))
        .source(SportsDbConstants.SOURCE_SPORTS_DB)
        .build());
    UUID deletedContentId = deletedContent.getId();
    contentRepository.delete(deletedContent);
    contentRepository.flush();

    given(sportsDbProperties.getTargetLeagueIds()).willReturn(List.of(LEAGUE_ID));
    given(sportsDbApiClient.getPastEventsByLeague(LEAGUE_ID)).willReturn(
        new SportsDbEventsResponse(List.of(event("100"), event("200"), event("300"))));

    JobExecution execution = jobLauncher.run(sportsDbPastEventsSyncJob, uniqueParams());

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

    Content updated = contentRepository.findByExternalIdAndSource(
        SportsDbConstants.externalId("100"), SportsDbConstants.SOURCE_SPORTS_DB).orElseThrow();
    assertThat(updated.getTitle()).isEqualTo("경기명100");

    Object[] deletedRow = (Object[]) entityManager.createNativeQuery(
            "SELECT title, deleted_at FROM contents WHERE external_id = :externalId")
        .setParameter("externalId", SportsDbConstants.externalId("200"))
        .getSingleResult();
    assertThat(deletedRow[0]).isEqualTo("원본 경기명");
    assertThat(deletedRow[1]).isNotNull();
    assertThat(contentTagRepository.findTagsByContentIds(List.of(deletedContentId))).isEmpty();

    Content created = contentRepository.findByExternalIdAndSource(
        SportsDbConstants.externalId("300"), SportsDbConstants.SOURCE_SPORTS_DB).orElseThrow();
    assertThat(created.getTitle()).isEqualTo("경기명300");
    List<ContentTagProjection> createdTags =
        contentTagRepository.findTagsByContentIds(List.of(created.getId()));
    assertThat(createdTags).extracting(ContentTagProjection::tagName).contains("Soccer");
  }

  private JobParameters uniqueParams() {
    return new JobParametersBuilder()
        .addLong("time", System.nanoTime())
        .toJobParameters();
  }

  private SportsDbEvent event(String id) {
    return new SportsDbEvent(id, "경기명" + id, "2026-01-01", "샘플 리그", "샘플 경기장", "https://sample.jpg");
  }

  private SportsDbEventsResponse sampleEventsResponse() {
    SportsDbEvent event = new SportsDbEvent(
        "1", "샘플 경기", "2026-01-01", "샘플 리그", "샘플 경기장", "https://sample.jpg");
    return new SportsDbEventsResponse(List.of(event));
  }
}
