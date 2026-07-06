package com.example.sb10_MoPl_team3.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
    @DisplayName("recalculate는 통계가 없으면 기본값으로 생성한 뒤 갱신한다")
    void recalculate_통계가_없으면_기본값_생성_후_갱신() {
        UUID contentId = UUID.randomUUID();
        Content content = content(contentId);

        given(contentStatsRepository.findByIdForUpdate(contentId)).willReturn(Optional.empty());
        given(contentRepository.getReferenceById(contentId)).willReturn(content);
        given(contentStatsRepository.save(any(ContentStats.class))).willAnswer(inv -> inv.getArgument(0));
        given(reviewRepository.countByContent_IdAndStatus(contentId, ReviewStatus.ACTIVE)).willReturn(1L);
        given(reviewRepository.findAverageRatingByContentIdAndStatus(contentId, ReviewStatus.ACTIVE))
                .willReturn(5.0);

        contentStatsService.recalculate(contentId);

        ArgumentCaptor<ContentStats> captor = ArgumentCaptor.forClass(ContentStats.class);
        then(contentStatsRepository).should().save(captor.capture());
        ContentStats saved = captor.getValue();
        assertThat(saved.getContent()).isEqualTo(content);
        assertThat(saved.getReviewCount()).isEqualTo(1);
        assertThat(saved.getAverageRating()).isEqualByComparingTo(new BigDecimal("5.00"));
    }

    @Test
    @DisplayName("ContentStats가 없는 콘텐츠에 기본 통계를 채워 넣는다")
    void backfillMissingStats_통계없는_콘텐츠에_기본값_저장() {
        Content content = content(UUID.randomUUID());
        given(contentRepository.findAllWithoutStats()).willReturn(List.of(content));

        contentStatsService.backfillMissingStats();

        ArgumentCaptor<List<ContentStats>> captor = ArgumentCaptor.forClass(List.class);
        then(contentStatsRepository).should().saveAll(captor.capture());

        List<ContentStats> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getContent()).isEqualTo(content);
        assertThat(saved.get(0).getAverageRating().doubleValue()).isEqualTo(0.0);
        assertThat(saved.get(0).getReviewCount()).isZero();
        assertThat(saved.get(0).getViewerCount()).isZero();
    }

    @Test
    @DisplayName("누락된 ContentStats가 없으면 저장을 호출하지 않는다")
    void backfillMissingStats_누락없으면_저장하지_않는다() {
        given(contentRepository.findAllWithoutStats()).willReturn(List.of());

        contentStatsService.backfillMissingStats();

        then(contentStatsRepository).should(never()).saveAll(any());
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
