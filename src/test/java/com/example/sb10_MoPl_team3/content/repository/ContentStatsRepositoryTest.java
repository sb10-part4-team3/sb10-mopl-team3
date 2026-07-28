package com.example.sb10_MoPl_team3.content.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.entity.ContentStats;
import com.example.sb10_MoPl_team3.global.config.JpaAuditingConfig;
import com.example.sb10_MoPl_team3.global.config.QuerydslConfig;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.transaction.TestTransaction;

@DataJpaTest
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
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

        assertThat(contentStatsRepository.incrementViewerCount(content.getId(), Instant.now()))
                .isEqualTo(1);
        assertThat(contentStatsRepository.decrementViewerCount(content.getId(), Instant.now()))
                .isEqualTo(1);
        assertThat(contentStatsRepository.decrementViewerCount(content.getId(), Instant.now()))
                .isZero();
        entityManager.clear();

        ContentStats stats = contentStatsRepository.findById(content.getId()).orElseThrow();
        assertThat(stats.getViewerCount()).isZero();
    }

    @Test
    @DisplayName("createDefaultIgnoringConflict은 통계가 없을 때만 기본값을 만든다")
    void createDefaultIgnoringConflict_통계가_없으면_생성() {
        Content content = contentRepository.save(Content.builder()
                .type(ContentType.MOVIE)
                .title("콘텐츠")
                .description("설명")
                .thumbnailUrl("thumbnail")
                .externalId("create-default-test-001")
                .source("test")
                .build());
        // createDefaultIgnoringConflict는 REQUIRES_NEW라 별도 커넥션에서 실행되므로,
        // 참조하는 Content가 실제로 커밋되어 보여야 한다.
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        contentStatsRepository.createDefaultIgnoringConflict(content.getId());
        entityManager.clear();

        ContentStats stats = contentStatsRepository.findById(content.getId()).orElseThrow();
        assertThat(stats.getAverageRating().doubleValue()).isEqualTo(0.0);
        assertThat(stats.getReviewCount()).isZero();
        assertThat(stats.getViewerCount()).isZero();
    }

    @Test
    @DisplayName("createDefaultIgnoringConflict은 이미 통계가 있으면 PK 충돌 예외를 던지고 기존 값은 그대로 유지된다")
    void createDefaultIgnoringConflict_이미_있으면_예외_기존값_유지() {
        Content content = contentRepository.save(Content.builder()
                .type(ContentType.MOVIE)
                .title("콘텐츠")
                .description("설명")
                .thumbnailUrl("thumbnail")
                .externalId("create-default-test-002")
                .source("test")
                .build());
        contentStatsRepository.saveAndFlush(
                ContentStats.builder().content(content).averageRating(new BigDecimal("4.50"))
                        .reviewCount(3).viewerCount(2).build());
        // 기존 행이 실제로 커밋되어 있어야 REQUIRES_NEW 트랜잭션에서 PK 충돌을 확인할 수 있다.
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        assertThatThrownBy(() -> contentStatsRepository.createDefaultIgnoringConflict(content.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        entityManager.clear();

        ContentStats stats = contentStatsRepository.findById(content.getId()).orElseThrow();
        assertThat(stats.getAverageRating()).isEqualByComparingTo(new BigDecimal("4.50"));
        assertThat(stats.getReviewCount()).isEqualTo(3);
    }
}
