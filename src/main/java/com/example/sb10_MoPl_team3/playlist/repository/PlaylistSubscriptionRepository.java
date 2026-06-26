package com.example.sb10_MoPl_team3.playlist.repository;

import com.example.sb10_MoPl_team3.playlist.entity.PlaylistSubscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlaylistSubscriptionRepository extends JpaRepository<PlaylistSubscriber, UUID> {
    boolean existsByPlaylistIdAndUserId(UUID playlistId, UUID userId);

    Optional<PlaylistSubscriber> findByPlaylistIdAndUserId(UUID playlistId, UUID userId);
}
