package com.example.sb10_MoPl_team3.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.dto.ContentCreateRequest;
import com.example.sb10_MoPl_team3.content.dto.ContentDto;
import com.example.sb10_MoPl_team3.content.dto.ContentUpdateRequest;
import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.entity.ContentStats;
import com.example.sb10_MoPl_team3.content.repository.ContentRepository;
import com.example.sb10_MoPl_team3.content.repository.ContentStatsRepository;
import com.example.sb10_MoPl_team3.content.repository.ContentTagRepository;
import com.example.sb10_MoPl_team3.global.cursor.CursorPageRequest;
import com.example.sb10_MoPl_team3.global.cursor.CursorResponse;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.global.file.FileStorageService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
class ContentServiceImplTest {

    @Mock private ContentRepository contentRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private ContentStatsRepository contentStatsRepository;
    @Mock private ContentTagRepository contentTagRepository;
    @Mock private ContentTagService contentTagService;

    @InjectMocks
    private ContentServiceImpl contentService;

    // --- create ---

    @Test
    void create_정상_등록_후_ContentDto_반환() {
        ContentCreateRequest request = new ContentCreateRequest(
            ContentType.MOVIE, "테스트 영화", "설명", List.of("액션")
        );

        given(contentRepository.save(any(Content.class))).willAnswer(inv -> {
            Content c = inv.getArgument(0);
            ReflectionTestUtils.setField(c, "id", UUID.randomUUID());
            return c;
        });
        given(contentTagService.syncTags(any(), eq(List.of("액션")))).willReturn(List.of("액션"));

        ContentDto result = contentService.create(request, null);

        assertThat(result.type()).isEqualTo(ContentType.MOVIE);
        assertThat(result.title()).isEqualTo("테스트 영화");
        assertThat(result.tags()).containsExactly("액션");
        then(contentRepository).should().save(any(Content.class));
        then(contentTagService).should().syncTags(any(), eq(List.of("액션")));
        then(contentStatsRepository).should().save(any(ContentStats.class));
    }

    @Test
    void create_썸네일_없이_등록하면_S3_업로드를_호출하지_않는다() {
        ContentCreateRequest request = new ContentCreateRequest(
            ContentType.TV_SERIES, "드라마", null, List.of()
        );

        given(contentRepository.save(any(Content.class))).willAnswer(inv -> {
            Content c = inv.getArgument(0);
            ReflectionTestUtils.setField(c, "id", UUID.randomUUID());
            return c;
        });
        given(contentTagService.syncTags(any(), any())).willReturn(List.of());

        contentService.create(request, null);

        then(fileStorageService).should(never()).upload(any());
    }

    // --- getContent ---

    @Test
    void getContent_정상_조회_시_ContentDto_반환() {
        Content content = buildContent(ContentType.MOVIE, "영화");
        ContentStats stats = buildStats(content, new BigDecimal("4.50"), 10, 200);

        given(contentRepository.findById(content.getId())).willReturn(Optional.of(content));
        given(contentStatsRepository.findById(content.getId())).willReturn(Optional.of(stats));
        given(contentTagRepository.findTagNamesByContentId(content.getId()))
            .willReturn(List.of("액션", "SF"));

        ContentDto result = contentService.getContent(content.getId());

        assertThat(result.id()).isEqualTo(content.getId());
        assertThat(result.type()).isEqualTo(ContentType.MOVIE);
        assertThat(result.title()).isEqualTo("영화");
        assertThat(result.averageRating()).isEqualTo(4.50);
        assertThat(result.reviewCount()).isEqualTo(10);
        assertThat(result.watcherCount()).isEqualTo(200L);
        assertThat(result.tags()).containsExactlyInAnyOrder("액션", "SF");
    }

    @Test
    void getContent_stats가_없으면_기본값으로_반환() {
        Content content = buildContent(ContentType.MOVIE, "영화");

        given(contentRepository.findById(content.getId())).willReturn(Optional.of(content));
        given(contentStatsRepository.findById(content.getId())).willReturn(Optional.empty());
        given(contentTagRepository.findTagNamesByContentId(any())).willReturn(List.of());

        ContentDto result = contentService.getContent(content.getId());

        assertThat(result.averageRating()).isEqualTo(0.0);
        assertThat(result.reviewCount()).isEqualTo(0);
        assertThat(result.watcherCount()).isEqualTo(0L);
    }

    @Test
    void getContent_존재하지_않는_ID_조회_시_예외() {
        UUID unknownId = UUID.randomUUID();
        given(contentRepository.findById(unknownId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> contentService.getContent(unknownId))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CONTENT_NOT_FOUND));
    }

    // --- updateContent ---

    @Test
    void updateContent_제목과_설명_정상_수정() {
        Content content = buildContent(ContentType.MOVIE, "원래 제목");
        ContentUpdateRequest request = new ContentUpdateRequest("새 제목", "새 설명", null);

        given(contentRepository.findById(content.getId())).willReturn(Optional.of(content));
        given(contentTagRepository.findTagNamesByContentId(content.getId())).willReturn(List.of());

        ContentDto result = contentService.updateContent(content.getId(), request);

        assertThat(result.title()).isEqualTo("새 제목");
        assertThat(result.description()).isEqualTo("새 설명");
    }

    @Test
    void updateContent_tags가_전달되면_syncTags를_호출한다() {
        Content content = buildContent(ContentType.MOVIE, "영화");
        ContentUpdateRequest request = new ContentUpdateRequest(null, null, List.of("로맨스"));

        given(contentRepository.findById(content.getId())).willReturn(Optional.of(content));
        given(contentTagService.syncTags(eq(content), eq(List.of("로맨스"))))
            .willReturn(List.of("로맨스"));

        ContentDto result = contentService.updateContent(content.getId(), request);

        assertThat(result.tags()).containsExactly("로맨스");
        then(contentTagService).should().syncTags(eq(content), eq(List.of("로맨스")));
    }

    @Test
    void updateContent_tags가_null이면_기존_태그를_그대로_유지한다() {
        Content content = buildContent(ContentType.MOVIE, "영화");
        ContentUpdateRequest request = new ContentUpdateRequest("새 제목", null, null);

        given(contentRepository.findById(content.getId())).willReturn(Optional.of(content));
        given(contentTagRepository.findTagNamesByContentId(content.getId()))
            .willReturn(List.of("기존태그"));

        ContentDto result = contentService.updateContent(content.getId(), request);

        assertThat(result.tags()).containsExactly("기존태그");
        then(contentTagService).should(never()).syncTags(any(), any());
    }

    @Test
    void updateContent_존재하지_않는_ID_수정_시_예외() {
        UUID unknownId = UUID.randomUUID();
        ContentUpdateRequest request = new ContentUpdateRequest("제목", null, null);

        given(contentRepository.findById(unknownId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> contentService.updateContent(unknownId, request))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CONTENT_NOT_FOUND));
    }

    // --- deleteContent ---

    @Test
    void deleteContent_정상_삭제_시_repository_delete_호출() {
        Content content = buildContent(ContentType.MOVIE, "영화");
        given(contentRepository.findById(content.getId())).willReturn(Optional.of(content));

        contentService.deleteContent(content.getId());

        then(contentRepository).should().delete(content);
    }

    @Test
    void deleteContent_존재하지_않는_ID_삭제_시_예외() {
        UUID unknownId = UUID.randomUUID();
        given(contentRepository.findById(unknownId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> contentService.deleteContent(unknownId))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CONTENT_NOT_FOUND));

        then(contentRepository).should(never()).delete(any());
    }

    // --- getContents ---

    @Test
    void getContents_목록_정상_조회_시_CursorResponse_반환() {
        Content content = buildContent(ContentType.MOVIE, "영화");
        ReflectionTestUtils.setField(content, "createdAt", Instant.now());
        CursorPageRequest pageRequest = new CursorPageRequest(null, null, 10, "createdAt", "ASC");

        given(contentRepository.findContentsByCursor(pageRequest, null, null, null))
            .willReturn(List.of(content));
        given(contentRepository.countContents(null, null, null)).willReturn(1L);
        given(contentStatsRepository.findAllById(any())).willReturn(List.of());
        given(contentTagRepository.findTagsByContentIds(any())).willReturn(List.of());

        CursorResponse<ContentDto> result = contentService.getContents(pageRequest, null, null, null);

        assertThat(result.data()).hasSize(1);
        assertThat(result.totalCount()).isEqualTo(1L);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.data().get(0).title()).isEqualTo("영화");
    }

    @Test
    void getContents_다음_페이지가_있으면_hasNext_true() {
        Content c1 = buildContent(ContentType.MOVIE, "영화1");
        Content c2 = buildContent(ContentType.MOVIE, "영화2");
        Content c3 = buildContent(ContentType.MOVIE, "영화3");
        Instant now = Instant.now();
        ReflectionTestUtils.setField(c1, "createdAt", now.minusSeconds(2));
        ReflectionTestUtils.setField(c2, "createdAt", now.minusSeconds(1));
        ReflectionTestUtils.setField(c3, "createdAt", now);

        CursorPageRequest pageRequest = new CursorPageRequest(null, null, 2, "createdAt", "ASC");

        given(contentRepository.findContentsByCursor(pageRequest, null, null, null))
            .willReturn(List.of(c1, c2, c3));  // size+1개 반환
        given(contentRepository.countContents(null, null, null)).willReturn(3L);
        given(contentStatsRepository.findAllById(any())).willReturn(List.of());
        given(contentTagRepository.findTagsByContentIds(any())).willReturn(List.of());

        CursorResponse<ContentDto> result = contentService.getContents(pageRequest, null, null, null);

        assertThat(result.data()).hasSize(2);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isNotNull();
    }

    // --- helpers ---

    private Content buildContent(ContentType type, String title) {
        Content content = Content.builder()
            .type(type)
            .title(title)
            .externalId(UUID.randomUUID().toString())
            .source("MANUAL")
            .build();
        ReflectionTestUtils.setField(content, "id", UUID.randomUUID());
        return content;
    }

    private ContentStats buildStats(Content content, BigDecimal rating, int reviewCount, int viewerCount) {
        ContentStats stats = ContentStats.builder()
            .content(content)
            .averageRating(rating)
            .reviewCount(reviewCount)
            .viewerCount(viewerCount)
            .build();
        ReflectionTestUtils.setField(stats, "id", content.getId());
        return stats;
    }
}
