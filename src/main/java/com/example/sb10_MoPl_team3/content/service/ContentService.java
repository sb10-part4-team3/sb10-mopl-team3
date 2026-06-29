package com.example.sb10_MoPl_team3.content.service;

import com.example.sb10_MoPl_team3.content.dto.ContentCreateRequest;
import com.example.sb10_MoPl_team3.content.dto.ContentDto;
import com.example.sb10_MoPl_team3.content.dto.ContentUpdateRequest;
import com.example.sb10_MoPl_team3.global.cursor.CursorPageRequest;
import com.example.sb10_MoPl_team3.global.cursor.CursorResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface ContentService {
  ContentDto create(ContentCreateRequest request, MultipartFile thumbnail);
  ContentDto getContent(UUID contentId);
  ContentDto updateContent(UUID contentId, ContentUpdateRequest request);
  CursorResponse<ContentDto> getContents(
      CursorPageRequest pageRequest,
      String typeEqual,
      String keywordLike,
      List<String> tagsIn
  );
}
