package com.example.sb10_MoPl_team3.content.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.sb10_MoPl_team3.content.entity.Content;

public interface ContentRepository extends JpaRepository<Content, UUID>, ContentRepositoryCustom {
  Optional<Content> findByExternalIdAndSource(String externalId, String source);
}
