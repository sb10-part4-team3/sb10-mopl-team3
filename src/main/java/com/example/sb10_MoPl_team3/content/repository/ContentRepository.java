package com.example.sb10_MoPl_team3.content.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.sb10_MoPl_team3.content.entity.Content;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentRepository extends JpaRepository<Content, UUID>, ContentRepositoryCustom {
  Optional<Content> findByExternalIdAndSource(String externalId, String source);

  @Query(value = "SELECT EXISTS(SELECT 1 FROM contents WHERE external_id = :externalId AND source = :source AND deleted_at IS NOT NULL)", nativeQuery = true)
  boolean existsDeletedByExternalIdAndSource(@Param("externalId") String externalId, @Param("source") String source);
}
