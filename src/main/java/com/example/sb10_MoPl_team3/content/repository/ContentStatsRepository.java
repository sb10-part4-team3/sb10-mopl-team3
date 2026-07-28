package com.example.sb10_MoPl_team3.content.repository;

import com.example.sb10_MoPl_team3.content.entity.ContentStats;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentStatsRepository extends JpaRepository<ContentStats, UUID>,
    ContentStatsRepositoryCustom {

    List<ContentStats> findByIdIn(Collection<UUID> ids);

    /**
     * 같은 콘텐츠에 대한 recalculate()가 동시에 실행될 때 통계값이 유실되지 않도록
     * 행 단위로 잠근 뒤 조회한다. 잠금을 먼저 걸어야 리뷰 집계 조회가 최신 커밋 상태를 보게 된다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ContentStats s WHERE s.id = :contentId")
    Optional<ContentStats> findByIdForUpdate(@Param("contentId") UUID contentId);

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

    @Modifying
    @Query("update ContentStats stats "
        + "set stats.viewerCount = :viewerCount, stats.updatedAt = :updatedAt "
        + "where stats.id = :contentId")
    int updateViewerCount(
        @Param("contentId") UUID contentId,
        @Param("viewerCount") int viewerCount,
        @Param("updatedAt") Instant updatedAt
    );
}
