package com.example.sb10_MoPl_team3.content.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.entity.ContentStats;
import com.example.sb10_MoPl_team3.content.entity.ContentTag;
import com.example.sb10_MoPl_team3.content.entity.Tag;
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
  private TagRepository tagRepository;

  @Autowired
  private ContentTagRepository contentTagRepository;

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
    assertThat(result).hasSize(2);
    assertThat(result)
        .extracting(Content::getTitle)
        .containsExactly("많은 시청자", "적은 시청자");

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
    List<Content> secondResult = contentRepository.findContentsByCursor(secondPage, null, null,
        null);

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
    List<Content> secondResult = contentRepository.findContentsByCursor(secondPage, null, null,
        null);

    // then - DESC에서 두번째보다 더 과거인 첫번째만 나와야 함
    assertThat(secondResult).hasSize(1);
    assertThat(secondResult.get(0).getTitle()).isEqualTo("첫번째");
  }

  @Test
  void findContentsByCursor_동일한_createdAt에서_id로_타이브레이크한다() {
    Content contentA = Content.builder()
        .type(ContentType.MOVIE)
        .title("동시생성A")
        .externalId("ext-201")
        .source("MANUAL")
        .build();
    contentRepository.save(contentA);
    em.flush();

    Content contentB = Content.builder()
        .type(ContentType.MOVIE)
        .title("동시생성B")
        .externalId("ext-202")
        .source("MANUAL")
        .build();
    contentRepository.save(contentB);
    em.flush();

    Instant sameTime = Instant.now();
    updateCreatedAt(contentA.getId(), sameTime);
    updateCreatedAt(contentB.getId(), sameTime);
    em.flush();
    em.clear();

    CursorPageRequest firstPage = new CursorPageRequest(null, null, 1, "createdAt", "ASC");
    List<Content> firstResult = contentRepository.findContentsByCursor(firstPage, null, null, null);

    // DB가 실제로 정렬한 순서를 "정답"으로 그대로 사용 (미리 추측 안 함)
    assertThat(firstResult).hasSize(2);
    Content firstItem = firstResult.get(0);
    Content secondItem = firstResult.get(1);

    // 1페이지의 첫 항목을 커서로 다음 페이지 요청
    CursorPageRequest secondPage = new CursorPageRequest(
        firstItem.getCreatedAt().toString(), firstItem.getId(), 1, "createdAt", "ASC"
    );
    List<Content> secondResult = contentRepository.findContentsByCursor(secondPage, null, null, null);

    // then - 두번째 항목만 나와야 함 (중복도 누락도 없이)
    assertThat(secondResult).hasSize(1);
    assertThat(secondResult.get(0).getId()).isEqualTo(secondItem.getId());
  }

  // ========================
  // 키워드 검색 단독 테스트
  // ========================

  @Test
  void findContentsByCursor_키워드로_제목_포함_콘텐츠만_조회한다() {
    saveContent("어벤져스 엔드게임", "k-001");
    saveContent("어벤져스 인피니티 워", "k-002");
    saveContent("아이언맨", "k-003");

    CursorPageRequest pageRequest = new CursorPageRequest(null, null, 10, "createdAt", "ASC");
    List<Content> result = contentRepository.findContentsByCursor(pageRequest, null, "어벤져스", null);

    assertThat(result).hasSize(2);
    assertThat(result).extracting(Content::getTitle)
        .containsExactlyInAnyOrder("어벤져스 엔드게임", "어벤져스 인피니티 워");
  }

  @Test
  void findContentsByCursor_키워드_대소문자_구분없이_검색된다() {
    saveContent("The Avengers", "k-004");
    saveContent("Iron Man", "k-005");

    CursorPageRequest pageRequest = new CursorPageRequest(null, null, 10, "createdAt", "ASC");
    List<Content> result = contentRepository.findContentsByCursor(pageRequest, null, "avengers", null);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getTitle()).isEqualTo("The Avengers");
  }

  @Test
  void findContentsByCursor_키워드_일치없으면_빈_결과를_반환한다() {
    saveContent("어벤져스", "k-006");

    CursorPageRequest pageRequest = new CursorPageRequest(null, null, 10, "createdAt", "ASC");
    List<Content> result = contentRepository.findContentsByCursor(pageRequest, null, "없는키워드XYZ", null);

    assertThat(result).isEmpty();
  }

  // ========================
  // 태그 필터링 단독 테스트
  // ========================

  @Test
  void findContentsByCursor_태그로_해당_콘텐츠만_필터링한다() {
    Content tagged = saveContent("액션영화", "t-001");
    saveContent("일반영화", "t-002");
    Tag actionTag = tagRepository.saveAndFlush(Tag.builder().name("액션").build());
    contentTagRepository.saveAndFlush(new ContentTag(tagged, actionTag));
    em.clear();

    CursorPageRequest pageRequest = new CursorPageRequest(null, null, 10, "createdAt", "ASC");
    List<Content> result = contentRepository.findContentsByCursor(pageRequest, null, null, List.of("액션"));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getTitle()).isEqualTo("액션영화");
  }

  @Test
  void findContentsByCursor_여러_태그_중_하나라도_있으면_조회된다() {
    Content sfContent = saveContent("SF영화", "t-003");
    Content actionContent = saveContent("액션영화", "t-004");
    saveContent("드라마", "t-005");

    Tag sfTag = tagRepository.saveAndFlush(Tag.builder().name("SF").build());
    Tag actionTag = tagRepository.saveAndFlush(Tag.builder().name("액션").build());
    contentTagRepository.saveAndFlush(new ContentTag(sfContent, sfTag));
    contentTagRepository.saveAndFlush(new ContentTag(actionContent, actionTag));
    em.clear();

    CursorPageRequest pageRequest = new CursorPageRequest(null, null, 10, "createdAt", "ASC");
    List<Content> result = contentRepository.findContentsByCursor(pageRequest, null, null, List.of("SF", "액션"));

    assertThat(result).hasSize(2);
    assertThat(result).extracting(Content::getTitle)
        .containsExactlyInAnyOrder("SF영화", "액션영화");
  }

  @Test
  void findContentsByCursor_태그_없는_콘텐츠는_태그_필터에서_제외된다() {
    saveContent("태그없는영화", "t-006");
    Content tagged = saveContent("태그있는영화", "t-007");
    Tag romanceTag = tagRepository.saveAndFlush(Tag.builder().name("로맨스").build());
    contentTagRepository.saveAndFlush(new ContentTag(tagged, romanceTag));
    em.clear();

    CursorPageRequest pageRequest = new CursorPageRequest(null, null, 10, "createdAt", "ASC");
    List<Content> result = contentRepository.findContentsByCursor(pageRequest, null, null, List.of("로맨스"));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getTitle()).isEqualTo("태그있는영화");
  }

  // ==================================
  // 키워드 + 태그 + 정렬 조합 테스트
  // ==================================

  @Test
  void findContentsByCursor_키워드와_태그_동시_필터링() {
    Content match = saveContent("어벤져스", "combo-001");
    saveContent("어벤져스2", "combo-002");        // 태그 없음 → 제외
    saveContent("아이언맨", "combo-003");          // 키워드 불일치 → 제외

    Tag actionTag = tagRepository.saveAndFlush(Tag.builder().name("액션").build());
    contentTagRepository.saveAndFlush(new ContentTag(match, actionTag));
    em.clear();

    CursorPageRequest pageRequest = new CursorPageRequest(null, null, 10, "createdAt", "ASC");
    List<Content> result = contentRepository.findContentsByCursor(pageRequest, null, "어벤져스", List.of("액션"));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getTitle()).isEqualTo("어벤져스");
  }

  @Test
  void findContentsByCursor_키워드와_rate_내림차순_정렬_조합() {
    Content highRated = saveContent("어벤져스 엔드게임", "combo-004");
    Content lowRated  = saveContent("어벤져스 인피니티 워", "combo-005");
    saveContent("아이언맨", "combo-006");          // 키워드 불일치 → 제외

    em.persist(ContentStats.builder().content(highRated).averageRating(new BigDecimal("4.80")).build());
    em.persist(ContentStats.builder().content(lowRated).averageRating(new BigDecimal("3.50")).build());
    em.flush();
    em.clear();

    CursorPageRequest pageRequest = new CursorPageRequest(null, null, 10, "rate", "DESC");
    List<Content> result = contentRepository.findContentsByCursor(pageRequest, null, "어벤져스", null);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getTitle()).isEqualTo("어벤져스 엔드게임");
    assertThat(result.get(1).getTitle()).isEqualTo("어벤져스 인피니티 워");
  }

  @Test
  void findContentsByCursor_태그와_watcherCount_내림차순_정렬_조합() {
    Content highView = saveContent("인기영화", "combo-007");
    Content lowView  = saveContent("비인기영화", "combo-008");
    Content noTag    = saveContent("태그없는영화", "combo-009");  // 태그 없음 → 제외

    Tag actionTag = tagRepository.saveAndFlush(Tag.builder().name("액션").build());
    contentTagRepository.saveAndFlush(new ContentTag(highView, actionTag));
    contentTagRepository.saveAndFlush(new ContentTag(lowView, actionTag));

    em.persist(ContentStats.builder().content(highView).viewerCount(500).build());
    em.persist(ContentStats.builder().content(lowView).viewerCount(100).build());
    em.persist(ContentStats.builder().content(noTag).viewerCount(999).build());
    em.flush();
    em.clear();

    CursorPageRequest pageRequest = new CursorPageRequest(null, null, 10, "watcherCount", "DESC");
    List<Content> result = contentRepository.findContentsByCursor(pageRequest, null, null, List.of("액션"));

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getTitle()).isEqualTo("인기영화");
    assertThat(result.get(1).getTitle()).isEqualTo("비인기영화");
  }

  @Test
  void findContentsByCursor_키워드와_태그와_createdAt_오름차순_정렬_조합() {
    Content older = saveContent("어벤져스 1편", "combo-010");
    Content newer = saveContent("어벤져스 2편", "combo-011");
    saveContent("스파이더맨", "combo-012");          // 키워드 불일치 → 제외

    Tag tag = tagRepository.saveAndFlush(Tag.builder().name("마블").build());
    contentTagRepository.saveAndFlush(new ContentTag(older, tag));
    contentTagRepository.saveAndFlush(new ContentTag(newer, tag));

    Instant now = Instant.now();
    updateCreatedAt(older.getId(), now.minusSeconds(120));
    updateCreatedAt(newer.getId(), now);
    em.flush();
    em.clear();

    CursorPageRequest pageRequest = new CursorPageRequest(null, null, 10, "createdAt", "ASC");
    List<Content> result = contentRepository.findContentsByCursor(pageRequest, null, "어벤져스", List.of("마블"));

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getTitle()).isEqualTo("어벤져스 1편");
    assertThat(result.get(1).getTitle()).isEqualTo("어벤져스 2편");
  }

  // ==============================
  // 커서 기반 페이지네이션 + 필터
  // ==============================

  @Test
  void findContentsByCursor_키워드_필터와_커서_페이지네이션_조합() {
    Content c1 = saveContent("마블 1편", "page-001");
    Content c2 = saveContent("마블 2편", "page-002");
    Content c3 = saveContent("마블 3편", "page-003");
    saveContent("DC 영화", "page-004");   // 키워드 불일치 → 항상 제외

    Instant now = Instant.now();
    updateCreatedAt(c1.getId(), now.minusSeconds(120));
    updateCreatedAt(c2.getId(), now.minusSeconds(60));
    updateCreatedAt(c3.getId(), now);
    em.flush();
    em.clear();

    // 1페이지: 마블 키워드, size=2 → c1·c2·c3 중 2+1개 반환
    CursorPageRequest firstPage = new CursorPageRequest(null, null, 2, "createdAt", "ASC");
    List<Content> firstResult = contentRepository.findContentsByCursor(firstPage, null, "마블", null);
    assertThat(firstResult).hasSize(3);

    // 2페이지: c2를 커서로 → c3만 반환
    Content cursor = contentRepository.findById(c2.getId()).orElseThrow();
    CursorPageRequest secondPage = new CursorPageRequest(
        cursor.getCreatedAt().toString(), cursor.getId(), 2, "createdAt", "ASC"
    );
    List<Content> secondResult = contentRepository.findContentsByCursor(secondPage, null, "마블", null);

    assertThat(secondResult).hasSize(1);
    assertThat(secondResult.get(0).getTitle()).isEqualTo("마블 3편");
  }

  @Test
  void findContentsByCursor_태그_필터와_커서_페이지네이션_조합() {
    Content c1 = saveContent("액션1", "page-005");
    Content c2 = saveContent("액션2", "page-006");
    Content c3 = saveContent("액션3", "page-007");
    saveContent("드라마", "page-008");   // 태그 없음 → 항상 제외

    Tag actionTag = tagRepository.saveAndFlush(Tag.builder().name("액션페이지").build());
    contentTagRepository.saveAndFlush(new ContentTag(c1, actionTag));
    contentTagRepository.saveAndFlush(new ContentTag(c2, actionTag));
    contentTagRepository.saveAndFlush(new ContentTag(c3, actionTag));

    Instant now = Instant.now();
    updateCreatedAt(c1.getId(), now.minusSeconds(120));
    updateCreatedAt(c2.getId(), now.minusSeconds(60));
    updateCreatedAt(c3.getId(), now);
    em.flush();
    em.clear();

    // 1페이지: size=2 → c1·c2·c3 중 2+1개 반환
    CursorPageRequest firstPage = new CursorPageRequest(null, null, 2, "createdAt", "ASC");
    List<Content> firstResult = contentRepository.findContentsByCursor(firstPage, null, null, List.of("액션페이지"));
    assertThat(firstResult).hasSize(3);

    // 2페이지: c2를 커서로 → c3만 반환 (noTag는 태그 필터에서 이미 제외)
    Content cursor = contentRepository.findById(c2.getId()).orElseThrow();
    CursorPageRequest secondPage = new CursorPageRequest(
        cursor.getCreatedAt().toString(), cursor.getId(), 2, "createdAt", "ASC"
    );
    List<Content> secondResult = contentRepository.findContentsByCursor(secondPage, null, null, List.of("액션페이지"));

    assertThat(secondResult).hasSize(1);
    assertThat(secondResult.get(0).getTitle()).isEqualTo("액션3");
  }

  private void updateCreatedAt(UUID id, Instant time) {
    em.createQuery("UPDATE Content c SET c.createdAt = :time WHERE c.id = :id")
        .setParameter("time", time)
        .setParameter("id", id)
        .executeUpdate();
  }

  private Content saveContent(String title, String externalId) {
    return contentRepository.saveAndFlush(Content.builder()
        .type(ContentType.MOVIE)
        .title(title)
        .externalId(externalId)
        .source("MANUAL")
        .build());
  }
}