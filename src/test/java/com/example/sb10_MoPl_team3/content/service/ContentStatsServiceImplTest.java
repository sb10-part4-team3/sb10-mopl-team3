package com.example.sb10_MoPl_team3.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.entity.ContentStats;
import com.example.sb10_MoPl_team3.content.repository.ContentRepository;
import com.example.sb10_MoPl_team3.content.repository.ContentStatsRepository;
import com.example.sb10_MoPl_team3.review.enums.ReviewStatus;
import com.example.sb10_MoPl_team3.review.repository.ReviewRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ContentStatsServiceImplTest {

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private ContentStatsRepository contentStatsRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ContentStatsServiceImpl contentStatsService;

    @Test
    @DisplayName("recalculate는 findByIdForUpdate로 잠근 뒤 리뷰 집계로 통계를 갱신한다")
    void recalculate_기존_통계를_잠금_조회_후_갱신() {
        UUID contentId = UUID.randomUUID();
        ContentStats stats = ContentStats.createDefault(content(contentId));

        given(contentStatsRepository.findByIdForUpdate(contentId)).willReturn(Optional.of(stats));
        given(reviewRepository.countByContent_IdAndStatus(contentId, ReviewStatus.ACTIVE)).willReturn(3L);
        given(reviewRepository.findAverageRatingByContentIdAndStatus(contentId, ReviewStatus.ACTIVE))
                .willReturn(4.567);

        contentStatsService.recalculate(contentId);

        then(contentStatsRepository).should().findByIdForUpdate(contentId);
        then(contentStatsRepository).should(never()).findById(any());
        assertThat(stats.getReviewCount()).isEqualTo(3);
        assertThat(stats.getAverageRating()).isEqualByComparingTo(new BigDecimal("4.57"));
    }

    @Test
    @DisplayName("recalculate는 리뷰가 없으면 평균을 0으로 갱신한다")
    void recalculate_리뷰가_없으면_평균은_0() {
        UUID contentId = UUID.randomUUID();
        ContentStats stats = ContentStats.createDefault(content(contentId));

        given(contentStatsRepository.findByIdForUpdate(contentId)).willReturn(Optional.of(stats));
        given(reviewRepository.countByContent_IdAndStatus(contentId, ReviewStatus.ACTIVE)).willReturn(0L);
        given(reviewRepository.findAverageRatingByContentIdAndStatus(contentId, ReviewStatus.ACTIVE))
                .willReturn(null);

        contentStatsService.recalculate(contentId);

        assertThat(stats.getReviewCount()).isZero();
        assertThat(stats.getAverageRating()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("recalculate는 통계가 없으면 원자적으로 만든 뒤 갱신한다")
    void recalculate_통계가_없으면_원자적으로_생성_후_갱신() {
        UUID contentId = UUID.randomUUID();
        ContentStats created = ContentStats.createDefault(content(contentId));

        given(contentStatsRepository.findByIdForUpdate(contentId))
                .willReturn(Optional.empty(), Optional.of(created));
        given(reviewRepository.countByContent_IdAndStatus(contentId, ReviewStatus.ACTIVE)).willReturn(1L);
        given(reviewRepository.findAverageRatingByContentIdAndStatus(contentId, ReviewStatus.ACTIVE))
                .willReturn(5.0);

        contentStatsService.recalculate(contentId);

        then(contentStatsRepository).should().createDefaultIgnoringConflict(contentId);
        then(contentStatsRepository).should(never()).save(any());
        assertThat(created.getReviewCount()).isEqualTo(1);
        assertThat(created.getAverageRating()).isEqualByComparingTo(new BigDecimal("5.00"));
    }

    @Test
    @DisplayName("다른 인스턴스가 동시에 먼저 만들면 PK 충돌 예외를 삼키고 그 행을 조회해서 이어서 쓴다")
    void recalculate_다른_인스턴스가_먼저_생성해도_그_행으로_갱신() {
        UUID contentId = UUID.randomUUID();
        ContentStats createdByOtherInstance = ContentStats.createDefault(content(contentId));

        given(contentStatsRepository.findByIdForUpdate(contentId))
                .willReturn(Optional.empty(), Optional.of(createdByOtherInstance));
        willThrow(new DataIntegrityViolationException("duplicate key"))
                .given(contentStatsRepository).createDefaultIgnoringConflict(contentId);
        given(reviewRepository.countByContent_IdAndStatus(contentId, ReviewStatus.ACTIVE)).willReturn(2L);
        given(reviewRepository.findAverageRatingByContentIdAndStatus(contentId, ReviewStatus.ACTIVE))
                .willReturn(3.0);

        contentStatsService.recalculate(contentId);

        assertThat(createdByOtherInstance.getReviewCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("생성 시도 후에도 조회가 안 되면 예외를 던진다")
    void recalculate_생성후_조회_실패시_예외() {
        UUID contentId = UUID.randomUUID();
        given(contentStatsRepository.findByIdForUpdate(contentId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> contentStatsService.recalculate(contentId))
                .isInstanceOf(IllegalStateException.class);

        then(contentStatsRepository).should().createDefaultIgnoringConflict(contentId);
    }

    @Test
    @DisplayName("이미 리뷰가 있던 콘텐츠는 0이 아니라 실제 리뷰를 집계한 값으로 채운다")
    void backfillMissingStats_리뷰가_있으면_집계값으로_채운다() {
        UUID contentId = UUID.randomUUID();
        Content content = content(contentId);
        ContentStats created = ContentStats.createDefault(content);

        given(contentRepository.findAllWithoutStats()).willReturn(List.of(content));
        given(contentStatsRepository.findByIdForUpdate(contentId))
                .willReturn(Optional.empty(), Optional.of(created));
        given(reviewRepository.countByContent_IdAndStatus(contentId, ReviewStatus.ACTIVE)).willReturn(5L);
        given(reviewRepository.findAverageRatingByContentIdAndStatus(contentId, ReviewStatus.ACTIVE))
                .willReturn(4.2);

        contentStatsService.backfillMissingStats();

        then(contentStatsRepository).should().createDefaultIgnoringConflict(contentId);
        assertThat(created.getReviewCount()).isEqualTo(5);
        assertThat(created.getAverageRating()).isEqualByComparingTo(new BigDecimal("4.20"));
    }

    @Test
    @DisplayName("리뷰가 없던 콘텐츠는 0으로 채운다")
    void backfillMissingStats_리뷰가_없으면_0으로_채운다() {
        UUID contentId = UUID.randomUUID();
        Content content = content(contentId);
        ContentStats created = ContentStats.createDefault(content);

        given(contentRepository.findAllWithoutStats()).willReturn(List.of(content));
        given(contentStatsRepository.findByIdForUpdate(contentId))
                .willReturn(Optional.empty(), Optional.of(created));
        given(reviewRepository.countByContent_IdAndStatus(contentId, ReviewStatus.ACTIVE)).willReturn(0L);
        given(reviewRepository.findAverageRatingByContentIdAndStatus(contentId, ReviewStatus.ACTIVE))
                .willReturn(null);

        contentStatsService.backfillMissingStats();

        assertThat(created.getReviewCount()).isZero();
        assertThat(created.getAverageRating()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("누락된 ContentStats가 없으면 아무 것도 계산하지 않는다")
    void backfillMissingStats_누락없으면_아무일도_하지않는다() {
        given(contentRepository.findAllWithoutStats()).willReturn(List.of());

        contentStatsService.backfillMissingStats();

        then(contentStatsRepository).should(never()).findByIdForUpdate(any());
        then(reviewRepository).should(never()).countByContent_IdAndStatus(any(), any());
    }

    private Content content(UUID id) {
        Content content = Content.builder()
                .type(ContentType.MOVIE)
                .title("content")
                .description("description")
                .thumbnailUrl("thumbnail")
                .externalId("external-" + id)
                .source("tmdb")
                .build();
        ReflectionTestUtils.setField(content, "id", id);
        return content;
    }
}
