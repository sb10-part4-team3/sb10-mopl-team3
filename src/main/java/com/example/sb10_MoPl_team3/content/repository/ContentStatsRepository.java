package com.example.sb10_MoPl_team3.content.repository;

import com.example.sb10_MoPl_team3.content.entity.ContentStats;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentStatsRepository extends JpaRepository<ContentStats, UUID> {

    List<ContentStats> findByIdIn(Collection<UUID> ids);
}
