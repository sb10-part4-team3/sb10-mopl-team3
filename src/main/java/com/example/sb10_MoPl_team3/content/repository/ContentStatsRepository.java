package com.example.sb10_MoPl_team3.content.repository;

import com.example.sb10_MoPl_team3.content.entity.ContentStats;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentStatsRepository extends JpaRepository<ContentStats, UUID> {

    List<ContentStats> findByIdIn(Collection<UUID> ids);

    @Modifying
    @Query("update ContentStats stats "
        + "set stats.viewerCount = stats.viewerCount + 1, stats.updatedAt = :updatedAt "
        + "where stats.id = :contentId")
    int incrementViewerCount(
        @Param("contentId") UUID contentId,
        @Param("updatedAt") Instant updatedAt
    );

    @Modifying
    @Query("update ContentStats stats "
        + "set stats.viewerCount = stats.viewerCount - 1, stats.updatedAt = :updatedAt "
        + "where stats.id = :contentId and stats.viewerCount > 0")
    int decrementViewerCount(
        @Param("contentId") UUID contentId,
        @Param("updatedAt") Instant updatedAt
    );
}
