package com.example.sb10_MoPl_team3.content.repository;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.global.cursor.CursorPageRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentRepositoryCustom {
  List<Content> findContentsByCursor(CursorPageRequest pageRequest, ContentType typeEqual, String keywordLike, List<String> tagsIn);
  long countContents(ContentType typeEqual, String keywordLike, List<String> tagsIn);
}