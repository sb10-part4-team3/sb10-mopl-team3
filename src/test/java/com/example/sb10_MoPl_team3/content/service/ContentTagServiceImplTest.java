package com.example.sb10_MoPl_team3.content.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.entity.Tag;
import com.example.sb10_MoPl_team3.content.repository.ContentRepository;
import com.example.sb10_MoPl_team3.content.repository.ContentTagRepository;
import com.example.sb10_MoPl_team3.content.repository.TagRepository;
import com.example.sb10_MoPl_team3.global.config.JpaAuditingConfig;
import com.example.sb10_MoPl_team3.global.config.QuerydslConfig;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({QuerydslConfig.class, JpaAuditingConfig.class})
class ContentTagServiceImplTest {

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private ContentTagRepository contentTagRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private EntityManager em;

    private ContentTagServiceImpl contentTagService;

    @BeforeEach
    void setUp() {
        contentTagService = new ContentTagServiceImpl(tagRepository, contentTagRepository);
    }

    // --- 콘텐츠 등록 시 태그 함께 등록 ---

    @Test
    void 콘텐츠_등록_시_새_태그가_생성되고_연결된다() {
        Content content = saveContent("ext-svc-001");

        List<String> result = contentTagService.syncTags(content, List.of("액션", "SF"));
        em.flush();
        em.clear();

        assertThat(result).containsExactlyInAnyOrder("액션", "SF");
        assertThat(contentTagRepository.findTagNamesByContentId(content.getId()))
            .containsExactlyInAnyOrder("액션", "SF");
    }

    @Test
    void 콘텐츠_등록_시_이미_존재하는_태그는_재활용하고_중복_생성하지_않는다() {
        tagRepository.saveAndFlush(Tag.builder().name("액션").build());
        Content content = saveContent("ext-svc-002");

        contentTagService.syncTags(content, List.of("액션"));
        em.flush();
        em.clear();

        assertThat(tagRepository.count()).isEqualTo(1);
        assertThat(contentTagRepository.findTagNamesByContentId(content.getId()))
            .containsExactly("액션");
    }

    @Test
    void 여러_콘텐츠가_동일한_태그를_공유할_수_있다() {
        Content contentA = saveContent("ext-svc-003");
        Content contentB = saveContent("ext-svc-004");

        contentTagService.syncTags(contentA, List.of("액션"));
        em.flush();
        contentTagService.syncTags(contentB, List.of("액션"));
        em.flush();
        em.clear();

        assertThat(tagRepository.count()).isEqualTo(1);
        assertThat(contentTagRepository.findTagNamesByContentId(contentA.getId()))
            .containsExactly("액션");
        assertThat(contentTagRepository.findTagNamesByContentId(contentB.getId()))
            .containsExactly("액션");
    }

    @Test
    void 중복_태그명_입력_시_distinct_처리되어_하나만_등록된다() {
        Content content = saveContent("ext-svc-005");

        List<String> result = contentTagService.syncTags(content, List.of("액션", "액션", "SF"));
        em.flush();
        em.clear();

        assertThat(result).containsExactlyInAnyOrder("액션", "SF");
        assertThat(contentTagRepository.findTagNamesByContentId(content.getId()))
            .containsExactlyInAnyOrder("액션", "SF");
    }

    // --- 콘텐츠 수정 시 태그 교체 ---

    @Test
    void 콘텐츠_수정_시_기존_태그가_제거되고_새_태그로_교체된다() {
        Content content = saveContent("ext-svc-006");
        contentTagService.syncTags(content, List.of("액션", "SF"));
        em.flush();

        contentTagService.syncTags(content, List.of("로맨스"));
        em.flush();
        em.clear();

        List<String> tagNames = contentTagRepository.findTagNamesByContentId(content.getId());
        assertThat(tagNames).containsExactly("로맨스");
        assertThat(tagNames).doesNotContain("액션", "SF");
    }

    @Test
    void 콘텐츠_수정_시_null_입력이면_기존_태그가_모두_삭제된다() {
        Content content = saveContent("ext-svc-007");
        contentTagService.syncTags(content, List.of("액션"));
        em.flush();

        contentTagService.syncTags(content, null);
        em.flush();
        em.clear();

        assertThat(contentTagRepository.findTagNamesByContentId(content.getId())).isEmpty();
    }

    @Test
    void 콘텐츠_수정_시_빈_리스트_입력이면_기존_태그가_모두_삭제된다() {
        Content content = saveContent("ext-svc-008");
        contentTagService.syncTags(content, List.of("액션"));
        em.flush();

        contentTagService.syncTags(content, List.of());
        em.flush();
        em.clear();

        assertThat(contentTagRepository.findTagNamesByContentId(content.getId())).isEmpty();
    }

    @Test
    void 콘텐츠_수정_시_다른_콘텐츠의_태그에_영향을_주지_않는다() {
        Content contentA = saveContent("ext-svc-009");
        Content contentB = saveContent("ext-svc-010");
        contentTagService.syncTags(contentA, List.of("액션"));
        contentTagService.syncTags(contentB, List.of("SF"));
        em.flush();

        contentTagService.syncTags(contentA, List.of("로맨스"));
        em.flush();
        em.clear();

        assertThat(contentTagRepository.findTagNamesByContentId(contentA.getId()))
            .containsExactly("로맨스");
        assertThat(contentTagRepository.findTagNamesByContentId(contentB.getId()))
            .containsExactly("SF");
    }

    // --- 콘텐츠 조회 시 태그 목록 함께 조회 ---

    @Test
    void 콘텐츠_조회_시_등록된_태그_목록이_함께_반환된다() {
        Content content = saveContent("ext-svc-011");
        contentTagService.syncTags(content, List.of("SF", "스릴러", "액션"));
        em.flush();
        em.clear();

        List<String> tagNames = contentTagRepository.findTagNamesByContentId(content.getId());

        assertThat(tagNames).hasSize(3);
        assertThat(tagNames).containsExactlyInAnyOrder("SF", "스릴러", "액션");
    }

    @Test
    void 태그가_없는_콘텐츠_조회_시_빈_목록이_반환된다() {
        Content content = saveContent("ext-svc-012");
        contentTagService.syncTags(content, List.of());
        em.flush();
        em.clear();

        List<String> tagNames = contentTagRepository.findTagNamesByContentId(content.getId());

        assertThat(tagNames).isEmpty();
    }

    private Content saveContent(String externalId) {
        return contentRepository.saveAndFlush(Content.builder()
            .type(ContentType.MOVIE)
            .title("테스트 콘텐츠")
            .externalId(externalId)
            .source("MANUAL")
            .build());
    }
}
