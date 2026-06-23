package com.example.sb10_MoPl_team3.content.service;

import com.example.sb10_MoPl_team3.content.dto.ContentCreateRequest;
import com.example.sb10_MoPl_team3.content.dto.ContentDto;
import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.repository.ContentRepository;
import com.example.sb10_MoPl_team3.global.file.FileStorageService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentServiceImpl implements ContentService {

  private final ContentRepository contentRepository;
  private final FileStorageService fileStorageService;

  @Override
  @Transactional
  public ContentDto create(ContentCreateRequest request, MultipartFile thumbnail) {

    String thumbnailUrl = null;
    if (thumbnail != null && !thumbnail.isEmpty()) {
      thumbnailUrl = fileStorageService.upload(thumbnail);
    }

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
