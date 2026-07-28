package com.example.sb10_MoPl_team3.content.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.sb10_MoPl_team3.content.entity.Tag;
import com.example.sb10_MoPl_team3.global.config.JpaAuditingConfig;
import com.example.sb10_MoPl_team3.global.config.QuerydslConfig;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@Import({QuerydslConfig.class, JpaAuditingConfig.class})
class TagRepositoryTest {

    @Autowired
    private TagRepository tagRepository;

    @Test
    void 태그를_저장하고_id로_조회할_수_있다() {
        Tag saved = tagRepository.saveAndFlush(Tag.builder().name("액션").build());

        Optional<Tag> result = tagRepository.findById(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("액션");
    }

    @Test
    void 태그를_이름으로_조회할_수_있다() {
        tagRepository.saveAndFlush(Tag.builder().name("SF").build());

        Optional<Tag> result = tagRepository.findByName("SF");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("SF");
    }

    @Test
    void 존재하지_않는_이름으로_조회하면_빈_Optional을_반환한다() {
        Optional<Tag> result = tagRepository.findByName("없는태그");

        assertThat(result).isEmpty();
    }

    @Test
    void 동일한_이름의_태그를_중복_저장하면_예외() {
        tagRepository.saveAndFlush(Tag.builder().name("로맨스").build());

        assertThatThrownBy(() ->
            tagRepository.saveAndFlush(Tag.builder().name("로맨스").build())
        ).isInstanceOf(DataIntegrityViolationException.class);
    }
}
