package com.example.sb10_MoPl_team3.content.repository;

import java.util.List;
import java.util.UUID;

public interface ContentTagRepositoryCustom {
  List<String> findTagNamesByContentId(UUID contentId);

}
