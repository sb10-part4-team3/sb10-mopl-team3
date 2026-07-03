package com.example.sb10_MoPl_team3.content.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.entity.ContentStats;
import com.example.sb10_MoPl_team3.global.config.JpaAuditingConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class ContentStatsRepositoryTest {

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private ContentStatsRepository contentStatsRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("시청자 수를 원자적으로 증가하고 0 미만으로 감소시키지 않는다")
    void updateViewerCount_atomicallyAndNeverBelowZero() {
        Content content = contentRepository.save(Content.builder()
                .type(ContentType.MOVIE)
                .title("콘텐츠")
                .description("설명")
                .thumbnailUrl("thumbnail")
                .externalId("viewer-count-test")
                .source("test")
                .build());
        contentStatsRepository.saveAndFlush(ContentStats.builder().content(content).build());
        entityManager.clear();

        assertThat(contentStatsRepository.incrementViewerCount(content.getId())).isEqualTo(1);
        assertThat(contentStatsRepository.decrementViewerCount(content.getId())).isEqualTo(1);
        assertThat(contentStatsRepository.decrementViewerCount(content.getId())).isZero();
        entityManager.clear();

        ContentStats stats = contentStatsRepository.findById(content.getId()).orElseThrow();
        assertThat(stats.getViewerCount()).isZero();
    }
}
