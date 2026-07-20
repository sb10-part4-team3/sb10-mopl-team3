package com.example.sb10_MoPl_team3.content.service;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.repository.ContentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContentCountCacheService {

  private final ContentRepository contentRepository;

  @Cacheable(cacheNames = "content-count", key = "#type + ':' + #keywordLike + ':' + #tagsIn")
  public long countContents(ContentType type, String keywordLike, List<String> tagsIn) {
    return contentRepository.countContents(type, keywordLike, tagsIn);
  }

}
