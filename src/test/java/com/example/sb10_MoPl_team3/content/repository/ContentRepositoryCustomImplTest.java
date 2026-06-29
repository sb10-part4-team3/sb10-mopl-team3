package com.example.sb10_MoPl_team3.content.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.entity.ContentStats;
import com.example.sb10_MoPl_team3.global.config.JpaAuditingConfig;
import com.example.sb10_MoPl_team3.global.config.QuerydslConfig;
import com.example.sb10_MoPl_team3.global.cursor.CursorPageRequest;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({QuerydslConfig.class, JpaAuditingConfig.class})
class ContentRepositoryCustomImplTest {

  @Autowired
  private ContentRepository contentRepository;

  @Autowired
  private EntityManager em;

  @Test
  void findContentsByCursor_조건없이_조회하면_저장된_콘텐츠가_나온다() {
    // given
    Content content = Content.builder()
        .type(ContentType.MOVIE)
        .title("테스트 영화")
        .externalId("ext-001")
        .source("MANUAL")
        .build();
    contentRepository.save(content);

    CursorPageRequest pageRequest = new CursorPageRequest(null, null, 10, "createdAt", "ASC");

    // when
    List<Content> result = contentRepository.findContentsByCursor(pageRequest, null, null, null);

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getTitle()).isEqualTo("테스트 영화");
  }

  @Test
  void findContentsByCursor_createdAt_오름차순_정렬() {
    // given
    Content older = Content.builder()
        .type(ContentType.MOVIE)
        .title("먼저 만든 콘텐츠")
        .externalId("ext-001")
        .source("MANUAL")
        .build();
    contentRepository.save(older);
    em.flush();

    Content newer = Content.builder()
        .type(ContentType.MOVIE)
        .title("나중에 만든 콘텐츠")
        .externalId("ext-002")
        .source("MANUAL")
        .build();
    contentRepository.save(newer);
    em.flush();

    em.createQuery("UPDATE Content c SET c.createdAt = :time WHERE c.id = :id")
        .setParameter("time", Instant.now().minusSeconds(60))
        .setParameter("id", older.getId())
        .executeUpdate();
    em.flush();
    em.clear();

    CursorPageRequest pageRequest = new CursorPageRequest(null, null, 10, "createdAt", "ASC");

    // when
    List<Content> result = contentRepository.findContentsByCursor(pageRequest, null, null, null);

    // then
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getTitle()).isEqualTo("먼저 만든 콘텐츠");
    assertThat(result.get(1).getTitle()).isEqualTo("나중에 만든 콘텐츠");
  }

  @Test
  void findContentsByCursor_createdAt_내림차순_정렬() {
    // given
    Content older = Content.builder()
        .type(ContentType.MOVIE)
        .title("먼저 만든 콘텐츠")
        .externalId("ext-003")
        .source("MANUAL")
        .build();
    contentRepository.save(older);
    em.flush();

    Content newer = Content.builder()
        .type(ContentType.MOVIE)
        .title("나중에 만든 콘텐츠")
        .externalId("ext-004")
        .source("MANUAL")
        .build();
    contentRepository.save(newer);
    em.flush();

    em.createQuery("UPDATE Content c SET c.createdAt = :time WHERE c.id = :id")
        .setParameter("time", Instant.now().minusSeconds(60))
        .setParameter("id", older.getId())
        .executeUpdate();
    em.flush();
    em.clear();

    CursorPageRequest pageRequest = new CursorPageRequest(null, null, 10, "createdAt", "DESC");

    // when
    List<Content> result = contentRepository.findContentsByCursor(pageRequest, null, null, null);

    // then
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getTitle()).isEqualTo("나중에 만든 콘텐츠");
    assertThat(result.get(1).getTitle()).isEqualTo("먼저 만든 콘텐츠");
  }

  @Test
  void findContentsByCursor_watcherCount_내림차순_정렬() {
    // given
    Content lowViews = Content.builder()
        .type(ContentType.MOVIE)
        .title("적은 시청자")
        .externalId("ext-005")
        .source("MANUAL")
        .build();
    contentRepository.save(lowViews);

    ContentStats lowStats = ContentStats.builder()
        .content(lowViews)
        .viewerCount(10)
        .build();
    em.persist(lowStats);

    Content highViews = Content.builder()
        .type(ContentType.MOVIE)
        .title("많은 시청자")
        .externalId("ext-006")
        .source("MANUAL")
        .build();
    contentRepository.save(highViews);

    ContentStats highStats = ContentStats.builder()
        .content(highViews)
        .viewerCount(100)
        .build();
    em.persist(highStats);

    em.flush();
    em.clear();

    CursorPageRequest pageRequest = new CursorPageRequest(null, null, 10, "watcherCount", "DESC");

    // when
    List<Content> result = contentRepository.findContentsByCursor(pageRequest, null, null, null);

    // then
    assertThat(result.get(0).getTitle()).isEqualTo("많은 시청자");
  }

  @Test
  void findContentsByCursor_rate_내림차순_정렬() {
    // given
    Content lowRated = Content.builder()
        .type(ContentType.MOVIE)
        .title("낮은 평점")
        .externalId("ext-007")
        .source("MANUAL")
        .build();
    contentRepository.save(lowRated);

    ContentStats lowStats = ContentStats.builder()
        .content(lowRated)
        .averageRating(new BigDecimal("2.50"))
        .build();
    em.persist(lowStats);

    Content highRated = Content.builder()
        .type(ContentType.MOVIE)
        .title("높은 평점")
        .externalId("ext-008")
        .source("MANUAL")
        .build();
    contentRepository.save(highRated);

    ContentStats highStats = ContentStats.builder()
        .content(highRated)
        .averageRating(new BigDecimal("4.80"))
        .build();
    em.persist(highStats);

    em.flush();
    em.clear();

    CursorPageRequest pageRequest = new CursorPageRequest(null, null, 10, "rate", "DESC");

    // when
    List<Content> result = contentRepository.findContentsByCursor(pageRequest, null, null, null);

    // then
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getTitle()).isEqualTo("높은 평점");
    assertThat(result.get(1).getTitle()).isEqualTo("낮은 평점");
  }

  @Test
  void findContentsByCursor_커서이후_데이터를_가져온다() {
    // given - 3개 생성, 시간차를 명확히 줌
    Content first = Content.builder()
        .type(ContentType.MOVIE)
        .title("첫번째")
        .externalId("ext-101")
        .source("MANUAL")
        .build();
    contentRepository.save(first);
    em.flush();

    Content second = Content.builder()
        .type(ContentType.MOVIE)
        .title("두번째")
        .externalId("ext-102")
        .source("MANUAL")
        .build();
    contentRepository.save(second);
    em.flush();

    Content third = Content.builder()
        .type(ContentType.MOVIE)
        .title("세번째")
        .externalId("ext-103")
        .source("MANUAL")
        .build();
    contentRepository.save(third);
    em.flush();

    // createdAt을 명확하게 순서대로 분리
    Instant now = Instant.now();
    updateCreatedAt(first.getId(), now.minusSeconds(120));
    updateCreatedAt(second.getId(), now.minusSeconds(60));
    updateCreatedAt(third.getId(), now);
    em.flush();
    em.clear();

    // 1페이지: 2개씩, ASC
    CursorPageRequest firstPage = new CursorPageRequest(null, null, 2, "createdAt", "ASC");
    List<Content> firstResult = contentRepository.findContentsByCursor(firstPage, null, null, null);

    // then - 1페이지 확인 (3개 중 2개 + 1개 더 가져와서 hasNext 판단용 = 3개 다 옴, size+1이니까)
    assertThat(firstResult).hasSize(3);

    // when - DB에서 다시 조회해서 갱신된 createdAt을 가져온 뒤, 그 항목을 커서로 다음 페이지 요청
    Content cursorItem = contentRepository.findById(second.getId()).orElseThrow();
    CursorPageRequest secondPage = new CursorPageRequest(
        cursorItem.getCreatedAt().toString(), cursorItem.getId(), 2, "createdAt", "ASC"
    );
    List<Content> secondResult = contentRepository.findContentsByCursor(secondPage, null, null, null);

    // then - 세번째만 나와야 함
    assertThat(secondResult).hasSize(1);
    assertThat(secondResult.get(0).getTitle()).isEqualTo("세번째");
  }

  @Test
  void findContentsByCursor_커서이후_데이터를_가져온다_내림차순() {
    // given - 3개 생성
    Content first = Content.builder()
        .type(ContentType.MOVIE)
        .title("첫번째")
        .externalId("ext-104")
        .source("MANUAL")
        .build();
    contentRepository.save(first);
    em.flush();

    Content second = Content.builder()
        .type(ContentType.MOVIE)
        .title("두번째")
        .externalId("ext-105")
        .source("MANUAL")
        .build();
    contentRepository.save(second);
    em.flush();

    Content third = Content.builder()
        .type(ContentType.MOVIE)
        .title("세번째")
        .externalId("ext-106")
        .source("MANUAL")
        .build();
    contentRepository.save(third);
    em.flush();

    Instant now = Instant.now();
    updateCreatedAt(first.getId(), now.minusSeconds(120));
    updateCreatedAt(second.getId(), now.minusSeconds(60));
    updateCreatedAt(third.getId(), now);
    em.flush();
    em.clear();

    // 1페이지: DESC면 세번째, 두번째, 첫번째 순
    CursorPageRequest firstPage = new CursorPageRequest(null, null, 2, "createdAt", "DESC");
    List<Content> firstResult = contentRepository.findContentsByCursor(firstPage, null, null, null);
    assertThat(firstResult).hasSize(3);

    // when - DB에서 다시 조회해서 갱신된 createdAt을 가져온 뒤, 그 항목(DESC 기준 1페이지의 마지막)을 커서로 다음 페이지 요청
    Content cursorItem = contentRepository.findById(second.getId()).orElseThrow();
    CursorPageRequest secondPage = new CursorPageRequest(
        cursorItem.getCreatedAt().toString(), cursorItem.getId(), 2, "createdAt", "DESC"
    );
    List<Content> secondResult = contentRepository.findContentsByCursor(secondPage, null, null, null);

    // then - DESC에서 두번째보다 더 과거인 첫번째만 나와야 함
    assertThat(secondResult).hasSize(1);
    assertThat(secondResult.get(0).getTitle()).isEqualTo("첫번째");
  }

  private void updateCreatedAt(UUID id, Instant time) {
    em.createQuery("UPDATE Content c SET c.createdAt = :time WHERE c.id = :id")
        .setParameter("time", time)
        .setParameter("id", id)
        .executeUpdate();
  }
}