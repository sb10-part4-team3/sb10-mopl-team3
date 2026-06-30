package com.example.sb10_MoPl_team3.content.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.entity.ContentTag;
import com.example.sb10_MoPl_team3.content.entity.Tag;
import com.example.sb10_MoPl_team3.global.config.JpaAuditingConfig;
import com.example.sb10_MoPl_team3.global.config.QuerydslConfig;
import com.example.sb10_MoPl_team3.global.cursor.CursorPageRequest;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({QuerydslConfig.class, JpaAuditingConfig.class})
class ContentRepositoryTest {

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private ContentTagRepository contentTagRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private EntityManager em;

    // --- composite unique (external_id + source) ---

    @Test
    void 동일한_externalId와_source_조합은_저장_시_예외() {
        contentRepository.saveAndFlush(buildContent(ContentType.MOVIE, "원본 영화", "ext-dup-001"));

        assertThatThrownBy(() ->
            contentRepository.saveAndFlush(buildContent(ContentType.TV_SERIES, "다른 제목", "ext-dup-001"))
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 동일한_externalId라도_source가_다르면_저장된다() {
        contentRepository.saveAndFlush(buildContent(ContentType.MOVIE, "TMDB 영화", "ext-uniq-001", "TMDB"));
        contentRepository.saveAndFlush(buildContent(ContentType.MOVIE, "MANUAL 영화", "ext-uniq-001", "MANUAL"));

        long count = contentRepository.countContents(null, null, null);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void 동일한_source라도_externalId가_다르면_저장된다() {
        contentRepository.saveAndFlush(buildContent(ContentType.MOVIE, "영화A", "ext-uniq-002", "TMDB"));
        contentRepository.saveAndFlush(buildContent(ContentType.MOVIE, "영화B", "ext-uniq-003", "TMDB"));

        long count = contentRepository.countContents(null, null, null);
        assertThat(count).isEqualTo(2);
    }

    // --- soft delete ---

    @Test
    void 삭제된_콘텐츠는_findById로_조회되지_않는다() {
        Content content = contentRepository.saveAndFlush(buildContent(ContentType.MOVIE, "삭제될 영화", "ext-del-001"));

        contentRepository.delete(content);
        em.flush();
        em.clear();

        Optional<Content> result = contentRepository.findById(content.getId());
        assertThat(result).isEmpty();
    }

    @Test
    void 삭제되지_않은_콘텐츠는_findById로_조회된다() {
        Content content = contentRepository.saveAndFlush(buildContent(ContentType.MOVIE, "정상 영화", "ext-del-002"));
        em.clear();

        Optional<Content> result = contentRepository.findById(content.getId());
        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("정상 영화");
    }

    // --- countContents ---

    @Test
    void countContents_필터없이_전체_카운트() {
        contentRepository.save(buildContent(ContentType.MOVIE, "영화1", "ext-cnt-001"));
        contentRepository.save(buildContent(ContentType.TV_SERIES, "드라마1", "ext-cnt-002"));
        em.flush();

        long count = contentRepository.countContents(null, null, null);

        assertThat(count).isEqualTo(2);
    }

    @Test
    void countContents_type_필터로_카운트() {
        contentRepository.save(buildContent(ContentType.MOVIE, "영화1", "ext-cnt-003"));
        contentRepository.save(buildContent(ContentType.MOVIE, "영화2", "ext-cnt-004"));
        contentRepository.save(buildContent(ContentType.TV_SERIES, "드라마1", "ext-cnt-005"));
        em.flush();

        long count = contentRepository.countContents("MOVIE", null, null);

        assertThat(count).isEqualTo(2);
    }

    @Test
    void countContents_keyword_필터로_카운트() {
        contentRepository.save(buildContent(ContentType.MOVIE, "어벤져스 엔드게임", "ext-cnt-006"));
        contentRepository.save(buildContent(ContentType.MOVIE, "아이언맨", "ext-cnt-007"));
        contentRepository.save(buildContent(ContentType.TV_SERIES, "왕좌의 게임", "ext-cnt-008"));
        em.flush();

        long count = contentRepository.countContents(null, "아이언", null);

        assertThat(count).isEqualTo(1);
    }

    @Test
    void countContents_keyword_대소문자_구분없이_카운트() {
        contentRepository.save(buildContent(ContentType.MOVIE, "The Avengers", "ext-cnt-009"));
        contentRepository.save(buildContent(ContentType.MOVIE, "Iron Man", "ext-cnt-010"));
        em.flush();

        long count = contentRepository.countContents(null, "avengers", null);

        assertThat(count).isEqualTo(1);
    }

    @Test
    void countContents_tags_필터로_카운트() {
        Content movie = contentRepository.saveAndFlush(buildContent(ContentType.MOVIE, "태그있는 영화", "ext-cnt-011"));
        contentRepository.saveAndFlush(buildContent(ContentType.TV_SERIES, "태그없는 드라마", "ext-cnt-012"));

        Tag actionTag = tagRepository.saveAndFlush(Tag.builder().name("액션").build());
        contentTagRepository.saveAndFlush(new ContentTag(movie, actionTag));
        em.clear();

        long count = contentRepository.countContents(null, null, List.of("액션"));

        assertThat(count).isEqualTo(1);
    }

    @Test
    void countContents_삭제된_콘텐츠는_카운트에서_제외된다() {
        Content toDelete = contentRepository.saveAndFlush(buildContent(ContentType.MOVIE, "삭제될 영화", "ext-cnt-013"));
        contentRepository.saveAndFlush(buildContent(ContentType.MOVIE, "남아있는 영화", "ext-cnt-014"));

        contentRepository.delete(toDelete);
        em.flush();
        em.clear();

        long count = contentRepository.countContents(null, null, null);

        assertThat(count).isEqualTo(1);
    }

    // --- findContentsByCursor filters ---

    @Test
    void findContentsByCursor_type_필터로_해당_타입만_조회() {
        contentRepository.save(buildContent(ContentType.MOVIE, "영화", "ext-flt-001"));
        contentRepository.save(buildContent(ContentType.TV_SERIES, "드라마", "ext-flt-002"));
        contentRepository.save(buildContent(ContentType.SPORT, "스포츠", "ext-flt-003"));
        em.flush();

        CursorPageRequest pageRequest = new CursorPageRequest(null, null, 10, "createdAt", "ASC");
        List<Content> result = contentRepository.findContentsByCursor(pageRequest, "MOVIE", null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo(ContentType.MOVIE);
    }

    @Test
    void findContentsByCursor_keyword_필터로_제목에_포함된_콘텐츠만_조회() {
        contentRepository.save(buildContent(ContentType.MOVIE, "어벤져스 엔드게임", "ext-flt-004"));
        contentRepository.save(buildContent(ContentType.MOVIE, "아이언맨", "ext-flt-005"));
        contentRepository.save(buildContent(ContentType.MOVIE, "스파이더맨", "ext-flt-006"));
        em.flush();

        CursorPageRequest pageRequest = new CursorPageRequest(null, null, 10, "createdAt", "ASC");
        List<Content> result = contentRepository.findContentsByCursor(pageRequest, null, "맨", null);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Content::getTitle)
            .containsExactlyInAnyOrder("아이언맨", "스파이더맨");
    }

    @Test
    void findContentsByCursor_keyword_대소문자_구분없이_조회() {
        contentRepository.save(buildContent(ContentType.MOVIE, "The Avengers", "ext-flt-007"));
        contentRepository.save(buildContent(ContentType.MOVIE, "Iron Man", "ext-flt-008"));
        em.flush();

        CursorPageRequest pageRequest = new CursorPageRequest(null, null, 10, "createdAt", "ASC");
        List<Content> result = contentRepository.findContentsByCursor(pageRequest, null, "avengers", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("The Avengers");
    }

    @Test
    void findContentsByCursor_tags_필터로_해당_태그_콘텐츠만_조회() {
        Content tagged = contentRepository.saveAndFlush(buildContent(ContentType.MOVIE, "SF 영화", "ext-flt-009"));
        contentRepository.saveAndFlush(buildContent(ContentType.MOVIE, "일반 영화", "ext-flt-010"));

        Tag sfTag = tagRepository.saveAndFlush(Tag.builder().name("SF").build());
        contentTagRepository.saveAndFlush(new ContentTag(tagged, sfTag));
        em.clear();

        CursorPageRequest pageRequest = new CursorPageRequest(null, null, 10, "createdAt", "ASC");
        List<Content> result = contentRepository.findContentsByCursor(pageRequest, null, null, List.of("SF"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("SF 영화");
    }

    @Test
    void findContentsByCursor_type과_keyword_복합_필터로_조회() {
        contentRepository.save(buildContent(ContentType.MOVIE, "액션 영화", "ext-flt-011"));
        contentRepository.save(buildContent(ContentType.MOVIE, "로맨스 영화", "ext-flt-012"));
        contentRepository.save(buildContent(ContentType.TV_SERIES, "액션 드라마", "ext-flt-013"));
        em.flush();

        CursorPageRequest pageRequest = new CursorPageRequest(null, null, 10, "createdAt", "ASC");
        List<Content> result = contentRepository.findContentsByCursor(pageRequest, "MOVIE", "액션", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("액션 영화");
    }

    private Content buildContent(ContentType type, String title, String externalId) {
        return buildContent(type, title, externalId, "MANUAL");
    }

    private Content buildContent(ContentType type, String title, String externalId, String source) {
        return Content.builder()
            .type(type)
            .title(title)
            .externalId(externalId)
            .source(source)
            .build();
    }
}
