package com.example.sb10_MoPl_team3.content.service;

import com.example.sb10_MoPl_team3.content.dto.ContentCreateRequest;
import com.example.sb10_MoPl_team3.content.dto.ContentDto;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface ContentService {
  ContentDto create(ContentCreateRequest request, MultipartFile thumbnail);
  ContentDto getContent(UUID contentId);
}
