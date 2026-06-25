package com.example.sb10_MoPl_team3.content.service;

import com.example.sb10_MoPl_team3.content.dto.ContentCreateRequest;
import com.example.sb10_MoPl_team3.content.dto.ContentDto;
import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.entity.ContentStats;
import com.example.sb10_MoPl_team3.content.mapper.ContentMapper;
import com.example.sb10_MoPl_team3.content.repository.ContentRepository;
import com.example.sb10_MoPl_team3.content.repository.ContentStatsRepository;
import com.example.sb10_MoPl_team3.content.repository.ContentTagRepository;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.global.file.FileStorageService;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
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

  public ContentServiceImpl(
      ContentRepository contentRepository,
      FileStorageService fileStorageService,
      PlatformTransactionManager transactionManager,
      ContentStatsRepository contentStatsRepository,
      ContentTagRepository contentTagRepository) {
    this.contentRepository = contentRepository;
    this.fileStorageService = fileStorageService;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
    this.contentStatsRepository = contentStatsRepository;
    this.contentTagRepository = contentTagRepository;
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
  public ContentDto getContent(UUID contentId) {
    Content content = contentRepository.findById(contentId)
        .filter(c -> c.getDeletedAt() == null)
        .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));

    ContentStats stats = contentStatsRepository.findById(contentId).orElse(null);
    List<String> tags = contentTagRepository.findTagNamesByContentId(contentId);

    return ContentMapper.toDto(content, stats, tags);
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

    return new ContentDto(
        savedContent.getId(),
        savedContent.getType(),
        savedContent.getTitle(),
        savedContent.getDescription(),
        savedContent.getThumbnailUrl(),
        tags, // TODO 태그엔티티 생성후 수정 필요
        0.0,
        0,
        0L
    );
  }


}
