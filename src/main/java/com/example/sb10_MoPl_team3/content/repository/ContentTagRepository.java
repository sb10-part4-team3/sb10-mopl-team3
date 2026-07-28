package com.example.sb10_MoPl_team3.content.repository;

import com.example.sb10_MoPl_team3.content.entity.ContentTag;
import com.example.sb10_MoPl_team3.content.entity.ContentTagId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentTagRepository extends JpaRepository<ContentTag, ContentTagId> ,
    ContentTagRepositoryCustom{

  @Query("""
    SELECT new com.example.sb10_MoPl_team3.content.repository.ContentTagProjection(ct.content.id, ct.tag.name)
    FROM ContentTag ct
    WHERE ct.content.id IN :contentIds
    """)
  List<ContentTagProjection> findTagsByContentIds(@Param("contentIds") List<UUID> contentIds);

  @Modifying
  @Query("DELETE FROM ContentTag ct WHERE ct.content.id = :contentId")
  void deleteAllByContentId(@Param("contentId") UUID contentId);

}
