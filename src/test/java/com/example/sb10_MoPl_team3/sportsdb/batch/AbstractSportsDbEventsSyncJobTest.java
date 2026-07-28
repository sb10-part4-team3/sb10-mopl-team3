package com.example.sb10_MoPl_team3.sportsdb.batch;

import static org.mockito.BDDMockito.given;

import com.example.sb10_MoPl_team3.content.repository.ContentRepository;
import com.example.sb10_MoPl_team3.content.repository.ContentTagRepository;
import com.example.sb10_MoPl_team3.sportsdb.SportsDbConstants;
import com.example.sb10_MoPl_team3.sportsdb.client.SportsDbApiClient;
import com.example.sb10_MoPl_team3.sportsdb.config.SportsDbProperties;
import com.example.sb10_MoPl_team3.sportsdb.dto.SportsDbEventsResponse.SportsDbEvent;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * SportsDb next/past 이벤트 동기화 Job 테스트가 공유하는 픽스처(정리 대상 externalId, DB 정리,
 * JobParameters 생성, 샘플 이벤트 생성)를 모아둔 베이스 클래스.
 */
@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
public abstract class AbstractSportsDbEventsSyncJobTest {

  protected static final String LEAGUE_ID = "4328";

  protected static final List<String> ALL_TEST_EXTERNAL_IDS = List.of(
      SportsDbConstants.externalId("1"),
      SportsDbConstants.externalId("10"), SportsDbConstants.externalId("11"),
      SportsDbConstants.externalId("100"), SportsDbConstants.externalId("200"),
      SportsDbConstants.externalId("300")
  );

  @Autowired
  protected JobLauncher jobLauncher;

  @Autowired
  protected ContentRepository contentRepository;

  @Autowired
  protected ContentTagRepository contentTagRepository;

  @Autowired
  protected EntityManager entityManager;

  @Autowired
  protected PlatformTransactionManager transactionManager;

  @MockitoBean
  protected SportsDbApiClient sportsDbApiClient;

  @MockitoBean
  protected SportsDbProperties sportsDbProperties;

  @AfterEach
  protected void cleanUp() {
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

  protected void stubTargetLeagueIds() {
    given(sportsDbProperties.getTargetLeagueIds()).willReturn(List.of(LEAGUE_ID));
  }

  protected JobParameters uniqueParams() {
    return new JobParametersBuilder()
        .addLong("time", System.nanoTime())
        .toJobParameters();
  }

  protected SportsDbEvent event(String id) {
    return new SportsDbEvent(id, "경기명" + id, "2026-01-01", "샘플 리그", "샘플 경기장", "https://sample.jpg");
  }
}
