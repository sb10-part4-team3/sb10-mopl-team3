package com.example.sb10_MoPl_team3.content.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.entity.ContentTag;
import com.example.sb10_MoPl_team3.content.entity.Tag;
import com.example.sb10_MoPl_team3.global.config.JpaAuditingConfig;
import com.example.sb10_MoPl_team3.global.config.QuerydslConfig;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({QuerydslConfig.class, JpaAuditingConfig.class})
class ContentTagRepositoryTest {

  @Autowired
  private ContentRepository contentRepository;

  @Autowired
  private ContentTagRepository contentTagRepository;

  @Autowired
  private TagRepository tagRepository;

  @Autowired
  private EntityManager em;

  @Test
  void ContentTag_저장_후_N대M_매핑이_정상적으로_연결된다() {
    Content content = saveContent("ext-ct-001");
    Tag tag = tagRepository.saveAndFlush(Tag.builder().name("액션").build());

    contentTagRepository.saveAndFlush(new ContentTag(content, tag));
    em.clear();

    List<String> tagNames = contentTagRepository.findTagNamesByContentId(content.getId());

    assertThat(tagNames).containsExactly("액션");
  }

  @Test
  void 하나의_콘텐츠에_여러_태그를_등록하고_모두_조회된다() {
    Content content = saveContent("ext-ct-002");
    Tag action = tagRepository.saveAndFlush(Tag.builder().name("액션").build());
    Tag sf = tagRepository.saveAndFlush(Tag.builder().name("SF").build());

    contentTagRepository.saveAndFlush(new ContentTag(content, action));
    contentTagRepository.saveAndFlush(new ContentTag(content, sf));
    em.clear();

    List<String> tagNames = contentTagRepository.findTagNamesByContentId(content.getId());

    assertThat(tagNames).containsExactlyInAnyOrder("액션", "SF");
  }

  @Test
  void 콘텐츠별로_각자의_태그만_조회된다() {
    Content contentA = saveContent("ext-ct-003");
    Content contentB = saveContent("ext-ct-004");
    Tag action = tagRepository.saveAndFlush(Tag.builder().name("액션").build());
    Tag romance = tagRepository.saveAndFlush(Tag.builder().name("로맨스").build());

    contentTagRepository.saveAndFlush(new ContentTag(contentA, action));
    contentTagRepository.saveAndFlush(new ContentTag(contentB, romance));
    em.clear();

    assertThat(contentTagRepository.findTagNamesByContentId(contentA.getId()))
        .containsExactly("액션");
    assertThat(contentTagRepository.findTagNamesByContentId(contentB.getId()))
        .containsExactly("로맨스");
  }

  @Test
  void findTagsByContentIds_배치_조회_시_각_콘텐츠의_태그가_함께_반환된다() {
    Content contentA = saveContent("ext-ct-005");
    Content contentB = saveContent("ext-ct-006");
    Tag action = tagRepository.saveAndFlush(Tag.builder().name("액션").build());
    Tag sf = tagRepository.saveAndFlush(Tag.builder().name("SF").build());

    contentTagRepository.saveAndFlush(new ContentTag(contentA, action));
    contentTagRepository.saveAndFlush(new ContentTag(contentB, sf));
    em.clear();

    List<ContentTagProjection> result = contentTagRepository.findTagsByContentIds(
        List.of(contentA.getId(), contentB.getId())
    );

    assertThat(result)
        .extracting(ContentTagProjection::contentId, ContentTagProjection::tagName)
        .containsExactlyInAnyOrder(
            tuple(contentA.getId(), "액션"),
            tuple(contentB.getId(), "SF")
        );
  }

  @Test
  void deleteAllByContentId_해당_콘텐츠_태그만_삭제하고_다른_콘텐츠_태그는_유지된다() {
    Content contentA = saveContent("ext-ct-007");
    Content contentB = saveContent("ext-ct-008");
    Tag action = tagRepository.saveAndFlush(Tag.builder().name("액션").build());
    Tag romance = tagRepository.saveAndFlush(Tag.builder().name("로맨스").build());

    contentTagRepository.saveAndFlush(new ContentTag(contentA, action));
    contentTagRepository.saveAndFlush(new ContentTag(contentB, romance));

    contentTagRepository.deleteAllByContentId(contentA.getId());
    em.flush();
    em.clear();

    assertThat(contentTagRepository.findTagNamesByContentId(contentA.getId())).isEmpty();
    assertThat(contentTagRepository.findTagNamesByContentId(contentB.getId()))
        .containsExactly("로맨스");
  }

  @Test
  void 태그가_없는_콘텐츠_조회_시_빈_리스트를_반환한다() {
    Content content = saveContent("ext-ct-009");
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
