package com.example.sb10_MoPl_team3.content.service;

import java.util.List;
import com.example.sb10_MoPl_team3.content.entity.Content;
import org.springframework.stereotype.Service;

@Service
public class ContentTagServiceImpl implements ContentTagService{

  @Override
  public List<String> syncTags(Content content, List<String> tagNames) {
    return List.of(); //컴파일을 위한 임시 구현
  }
}
