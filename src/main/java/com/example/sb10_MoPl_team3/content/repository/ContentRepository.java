package com.example.sb10_MoPl_team3.content.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.example.sb10_MoPl_team3.content.entity.Content;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ContentRepository extends JpaRepository<Content, UUID>, ContentRepositoryCustom {
  Optional<Content> findByExternalIdAndSource(String externalId, String source);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT c FROM Content c WHERE c.id = :contentId")
  Optional<Content> findByIdForUpdate(@Param("contentId") UUID contentId);

  @Query(value = "SELECT EXISTS(SELECT 1 FROM contents WHERE external_id = :externalId AND source = :source AND deleted_at IS NOT NULL)", nativeQuery = true)
  boolean existsDeletedByExternalIdAndSource(@Param("externalId") String externalId, @Param("source") String source);

  @Query("SELECT c FROM Content c WHERE c.id NOT IN (SELECT s.id FROM ContentStats s)")
  List<Content> findAllWithoutStats();
}
