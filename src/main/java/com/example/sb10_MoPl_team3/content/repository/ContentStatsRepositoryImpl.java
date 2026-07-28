package com.example.sb10_MoPl_team3.content.repository;

import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.entity.ContentStats;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class ContentStatsRepositoryImpl implements ContentStatsRepositoryCustom {

  private final EntityManager entityManager;

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void createDefaultIgnoringConflict(UUID contentId) {
    Content content = entityManager.getReference(Content.class, contentId);
    entityManager.persist(ContentStats.createDefault(content));
    // 이 트랜잭션 안에서 바로 PK 충돌 여부를 확인하기 위해 즉시 flush한다.
    entityManager.flush();
  }
}
