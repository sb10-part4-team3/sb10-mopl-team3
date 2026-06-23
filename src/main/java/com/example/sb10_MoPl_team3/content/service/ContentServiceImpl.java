package com.example.sb10_MoPl_team3.content.service;

import com.example.sb10_MoPl_team3.content.dto.ContentCreateRequest;
import com.example.sb10_MoPl_team3.content.dto.ContentDto;
import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentServiceImpl implements ContentService{

  private final ContentRepository contentRepository;

  @Override
  @Transactional
  public ContentDto create(ContentCreateRequest request) {
    return null;
  }
}
