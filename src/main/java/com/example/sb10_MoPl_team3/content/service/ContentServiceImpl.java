package com.example.sb10_MoPl_team3.content.service;

import com.example.sb10_MoPl_team3.content.dto.ContentCreateRequest;
import com.example.sb10_MoPl_team3.content.dto.ContentDto;
import com.example.sb10_MoPl_team3.content.dto.ContentUpdateRequest;
import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.entity.ContentStats;
import com.example.sb10_MoPl_team3.content.mapper.ContentMapper;
import com.example.sb10_MoPl_team3.content.repository.ContentRepository;
import com.example.sb10_MoPl_team3.content.repository.ContentStatsRepository;
import com.example.sb10_MoPl_team3.content.repository.ContentTagProjection;
import com.example.sb10_MoPl_team3.content.repository.ContentTagRepository;
import com.example.sb10_MoPl_team3.global.cursor.CursorPageRequest;
import com.example.sb10_MoPl_team3.global.cursor.CursorResponse;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.global.file.FileStorageService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class ContentServiceImpl implements ContentService {

  private final ContentRepository contentRepository;
  private final FileStorageService fileStorageService;
  // S3 업로드와 DB 저장을 같은 @Transactional 메서드에 두면
  //  1) S3 업로드(동기 네트워크 I/O) 동안 DB 커넥션/트랜잭션이 불필요하게 점유되고
  //  2) S3 업로드 성공 후 DB 커밋이 실패하면 S3 파일만 남는 고아 파일이 발생한다.
  // 트랜잭션 경계를 직접 제어하기 위해 TransactionTemplate을 사용해
  // "DB 저장"만 짧게 트랜잭션으로 감싼다.
  private final TransactionTemplate transactionTemplate;
  private final ContentStatsRepository contentStatsRepository;
  private final ContentTagRepository contentTagRepository;
  private final ContentTagService contentTagService;

  public ContentServiceImpl(
      ContentRepository contentRepository,
      FileStorageService fileStorageService,
      PlatformTransactionManager transactionManager,
      ContentStatsRepository contentStatsRepository,
      ContentTagRepository contentTagRepository,
      ContentTagService contentTagService) {
    this.contentRepository = contentRepository;
    this.fileStorageService = fileStorageService;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
    this.contentStatsRepository = contentStatsRepository;
    this.contentTagRepository = contentTagRepository;
    this.contentTagService = contentTagService;
  }

  @Override
  public ContentDto create(ContentCreateRequest request, MultipartFile thumbnail) {

    // 1. S3 업로드는 트랜잭션을 시작하기 전에 수행한다.
    //    -> 느린 네트워크 I/O 동안 DB 커넥션을 점유하지 않는다.
    String thumbnailUrl = null;
    if (thumbnail != null && !thumbnail.isEmpty()) {
      thumbnailUrl = fileStorageService.upload(thumbnail);
    }

    String uploadedThumbnailUrl = thumbnailUrl;
    try {
      // 2. DB 저장만 별도의 짧은 트랜잭션으로 처리한다.
      return transactionTemplate.execute(status -> saveContent(request, uploadedThumbnailUrl));
    } catch (RuntimeException e) {
      // 3. DB 트랜잭션이 실패해 롤백되더라도 S3에는 이미 파일이 업로드되어 있으므로,
      //    고아 파일로 남지 않도록 업로드한 파일을 보상 삭제(compensating delete)한다.
      if (thumbnailUrl != null) {
        fileStorageService.deleteByUrl(thumbnailUrl);
      }
      throw e;
    }
  }

  @Override
  @Transactional(readOnly = true)
  public ContentDto getContent(UUID contentId) {
    Content content = contentRepository.findById(contentId)
        .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));

    ContentStats stats = contentStatsRepository.findById(contentId).orElse(null);
    List<String> tags = contentTagRepository.findTagNamesByContentId(contentId);

    return ContentMapper.toDto(content, stats, tags);
  }

  @Override
  @Transactional
  public ContentDto updateContent(UUID contentId, ContentUpdateRequest request) {
    Content content = contentRepository.findById(contentId)
        .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));

    content.update(request.title(), request.description());

    List<String> tags = contentTagService.syncTags(content, request.tags());

    ContentStats stats = contentStatsRepository.findById(contentId)
        .orElse(null);

    return ContentMapper.toDto(content, stats, tags);
  }


  // ContentStats는 Content soft delete 시 별도로 삭제하지 않고 그대로 유지한다.
  // - ContentStats에는 deletedAt 컬럼이 없어 자체적인 soft delete가 불가능함
  // - Content의 @SQLRestriction("deleted_at IS NULL")이 조회를 차단하므로,
  //   ContentStats가 API 응답에 노출될 일은 없음 (Content를 거쳐서만 접근 가능한 구조)
  @Override
  @Transactional
  public void deleteContent(UUID contentId) {
    Content content = contentRepository.findById(contentId)
        .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));

    contentRepository.delete(content);
  }

  @Override
  @Transactional(readOnly = true)
  public CursorResponse<ContentDto> getContents(
      CursorPageRequest pageRequest,
      String typeEqual,
      String keywordLike,
      List<String> tagsIn
  ) {
    // 1. Repository에서 콘텐츠 목록 조회 (size+1개, 정렬/커서 적용됨)
    List<Content> contents = contentRepository.findContentsByCursor(
        pageRequest, typeEqual, keywordLike, tagsIn
    );

    // 2. 전체 개수 조회 (필터 조건만 적용)
    long totalCount = contentRepository.countContents(typeEqual, keywordLike, tagsIn);

    // 3. ContentStats, Tag 배치 조회 (N+1 방지)
    List<UUID> contentIds = contents.stream().map(Content::getId).toList();

    Map<UUID, ContentStats> statsMap = contentStatsRepository.findAllById(contentIds).stream()
        .collect(Collectors.toMap(ContentStats::getId, s -> s));

    Map<UUID, List<String>> tagsMap = contentTagRepository.findTagsByContentIds(contentIds).stream()
        .collect(Collectors.groupingBy(
            ContentTagProjection::contentId,
            Collectors.mapping(ContentTagProjection::tagName, Collectors.toList())
        ));

    // 4. Content 기준으로 커서 정보 계산 (sortBy에 맞는 값 추출)
    CursorResponse<Content> cursorInfo = CursorResponse.of(
        contents,
        pageRequest.size(),
        totalCount,
        pageRequest.sortBy(),
        pageRequest.sortDirection(),
        content -> extractSortValue(content, statsMap.get(content.getId()), pageRequest.sortBy()),
        Content::getId
    );

    // 5. 잘라낸 data(size개)만 DTO로 변환
    List<ContentDto> dtos = cursorInfo.data().stream()
        .map(c -> ContentMapper.toDto(c, statsMap.get(c.getId()), tagsMap.getOrDefault(c.getId(), List.of())))
        .toList();

    // 6. DTO로 교체한 최종 CursorResponse 반환
    return new CursorResponse<>(
        dtos,
        cursorInfo.nextCursor(),
        cursorInfo.nextIdAfter(),
        cursorInfo.hasNext(),
        cursorInfo.totalCount(),
        cursorInfo.sortBy(),
        cursorInfo.sortDirection()
    );
  }

  /**
   * sortBy 기준에 맞는 값을 문자열로 추출한다 (다음 페이지 커서로 쓰기 위함).
   * Content 자체 컬럼(createdAt)이거나, ContentStats의 값(watcherCount, rate)일 수 있다.
   */
  private String extractSortValue(Content content, ContentStats stats, String sortBy) {
    return switch (sortBy) {
      case "watcherCount" -> String.valueOf(stats != null ? stats.getViewerCount() : 0);
      case "rate" -> String.valueOf(stats != null ? stats.getAverageRating() : BigDecimal.ZERO);
      default -> content.getCreatedAt().toString();
    };
  }

  private ContentDto saveContent(ContentCreateRequest request, String thumbnailUrl) {
    List<String> tags = request.tags() != null ? request.tags() : List.of(); // TODO 태그엔티티 생성후 수정 필요

    Content content = Content.builder()
        .type(request.type())
        .title(request.title())
        .description(request.description())
        .thumbnailUrl(thumbnailUrl)
        .externalId(UUID.randomUUID().toString()) // TODO 외부 API 연동시 수정 필요
        .source("MANUAL")  // // TODO 외부 API 연동시 수정 필요
        .build();

    Content savedContent = contentRepository.save(content);

    ContentStats stats = ContentStats.builder()
        .content(savedContent)
        .averageRating(BigDecimal.ZERO)
        .reviewCount(0)
        .viewerCount(0)
        .build();
    contentStatsRepository.save(stats);

    return new ContentDto(
        savedContent.getId(),
        savedContent.getType(),
        savedContent.getTitle(),
        savedContent.getDescription(),
        savedContent.getThumbnailUrl(),
        tags, // TODO 태그엔티티 생성후 수정 필요
        stats.getAverageRating().doubleValue(),
        stats.getReviewCount(),
        (long) stats.getViewerCount()
    );
  }
}