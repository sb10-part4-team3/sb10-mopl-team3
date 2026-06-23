package com.example.sb10_MoPl_team3.content.service;

import com.example.sb10_MoPl_team3.content.dto.ContentCreateRequest;
import com.example.sb10_MoPl_team3.content.dto.ContentDto;

public interface ContentService {
  ContentDto create(ContentCreateRequest request);

}
