package com.example.sb10_MoPl_team3.sportsdb.service;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SportsDbContentSyncServiceIntegrationTest {

  private static final String TEST_LEAGUE_ID = "9999";

  private static final String DELETED_EXTERNAL_ID = SportsDbConstants.externalId("100");
  private static final String UPDATED_EXTERNAL_ID = SportsDbConstants.externalId("200");
  private static final String NEW_EXTERNAL_ID = SportsDbConstants.externalId("300");

  @Autowired
  private SportsDbContentSyncService sportsDbContentSyncService;

  @Autowired
  private ContentRepository contentRepository;

  @Autowired
  private ContentTagRepository contentTagRepository;

  @Autowired
  private EntityManager entityManager;

  @MockitoBean
  private SportsDbApiClient sportsDbApiClient;

  @MockitoBean
  private SportsDbProperties sportsDbProperties;

  @AfterEach
  void cleanUp() {
    // REQUIRES_NEW로 커밋된 데이터는 테스트 트랜잭션 롤백으로 지워지지 않으므로 직접 삭제한다
    List<String> ids = List.of(DELETED_EXTERNAL_ID, UPDATED_EXTERNAL_ID, NEW_EXTERNAL_ID);
    entityManager.createNativeQuery(
            "DELETE FROM content_tags WHERE content_id IN (SELECT id FROM contents WHERE external_id IN (:ids))")
        .setParameter("ids", ids)
        .executeUpdate();

    entityManager.createNativeQuery(
            "DELETE FROM content_stats WHERE content_id IN (SELECT id FROM contents WHERE external_id IN (:ids))")
        .setParameter("ids", ids)
        .executeUpdate();

    entityManager.createNativeQuery("DELETE FROM contents WHERE external_id IN (:ids)")
        .setParameter("ids", ids)
        .executeUpdate();
  }

  @Test
  @DisplayName("소프트 삭제된 경기는 갱신을 건너뛰고, 나머지 경기는 정상적으로 동기화된다")
  void syncNextEvents_소프트삭제된_경기는_건너뛰고_나머지는_동기화된다() {
    // given: 소프트 삭제된 콘텐츠
    Content deletedContent = contentRepository.save(Content.builder()
        .type(ContentType.SPORT)
        .title("원본 제목")
        .description("원본 설명")
        .thumbnailUrl("https://old.jpg")
        .externalId(DELETED_EXTERNAL_ID)
        .source(SportsDbConstants.SOURCE_SPORTS_DB)
        .build());
    UUID deletedContentId = deletedContent.getId();
    contentRepository.delete(deletedContent);
    contentRepository.flush();

    // given: 이미 존재하는(삭제되지 않은) 콘텐츠 - 갱신 대상
    contentRepository.save(Content.builder()
        .type(ContentType.SPORT)
        .title("갱신 전 제목")
        .description("갱신 전 설명")
        .thumbnailUrl("https://before.jpg")
        .externalId(UPDATED_EXTERNAL_ID)
        .source(SportsDbConstants.SOURCE_SPORTS_DB)
        .build());

    // given: upsert가 REQUIRES_NEW로 별도 트랜잭션에서 동작하므로, setup 데이터를 커밋해야 보인다
    TestTransaction.flagForCommit();
    TestTransaction.end();
    TestTransaction.start();
    TestTransaction.flagForCommit();

    given(sportsDbProperties.getTargetLeagueIds()).willReturn(List.of(TEST_LEAGUE_ID));

    SportsDbEvent deletedEvent = new SportsDbEvent(
        "100", "SportsDB가 보낸 새 이벤트명", "2026-01-01", "K리그1", "서울월드컵경기장", "https://new.jpg");
    SportsDbEvent updatedEvent = new SportsDbEvent(
        "200", "갱신 후 경기명", "2026-02-01", "K리그1", "인천축구전용경기장", "https://updated.jpg");
    SportsDbEvent newEvent = new SportsDbEvent(
        "300", "새 경기명", "2026-03-01", "K리그1", "수원월드컵경기장", "https://new3.jpg");

    given(sportsDbApiClient.getNextEventsByLeague(TEST_LEAGUE_ID))
        .willReturn(new SportsDbEventsResponse(List.of(deletedEvent, updatedEvent, newEvent)));

    // when
    sportsDbContentSyncService.syncNextEvents();
    entityManager.flush();
    entityManager.clear();

    // then: 소프트 삭제된 콘텐츠는 갱신되지 않는다 (title/description 원본 유지, deleted_at 유지)
    Object[] deletedRow = (Object[]) entityManager.createNativeQuery(
            "SELECT title, description, deleted_at FROM contents WHERE external_id = :externalId AND source = :source")
        .setParameter("externalId", DELETED_EXTERNAL_ID)
        .setParameter("source", SportsDbConstants.SOURCE_SPORTS_DB)
        .getSingleResult();

    assertThat(deletedRow[0]).isEqualTo("원본 제목");
    assertThat(deletedRow[1]).isEqualTo("원본 설명");
    assertThat(deletedRow[2]).isNotNull();

    List<ContentTagProjection> deletedContentTags =
        contentTagRepository.findTagsByContentIds(List.of(deletedContentId));
    assertThat(deletedContentTags).isEmpty();

    // then: 기존 경기는 정상적으로 갱신된다
    Content updated = contentRepository.findByExternalIdAndSource(UPDATED_EXTERNAL_ID, SportsDbConstants.SOURCE_SPORTS_DB)
        .orElseThrow();
    assertThat(updated.getTitle()).isEqualTo("갱신 후 경기명");
    assertThat(updated.getDescription()).isEqualTo("K리그1 2026-02-01");

    List<ContentTagProjection> updatedContentTags =
        contentTagRepository.findTagsByContentIds(List.of(updated.getId()));
    assertThat(updatedContentTags).extracting(ContentTagProjection::tagName)
        .contains("스포츠", "인천축구전용경기장");

    // then: 새 경기는 정상적으로 저장되고 태그가 붙는다
    Content created = contentRepository.findByExternalIdAndSource(NEW_EXTERNAL_ID, SportsDbConstants.SOURCE_SPORTS_DB)
        .orElseThrow();
    assertThat(created.getTitle()).isEqualTo("새 경기명");
    assertThat(created.getDescription()).isEqualTo("K리그1 2026-03-01");
    assertThat(created.getType()).isEqualTo(ContentType.SPORT);

    List<ContentTagProjection> createdContentTags =
        contentTagRepository.findTagsByContentIds(List.of(created.getId()));
    assertThat(createdContentTags).extracting(ContentTagProjection::tagName)
        .contains("스포츠",  "수원월드컵경기장");
  }
}
