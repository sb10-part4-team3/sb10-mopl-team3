package com.example.sb10_MoPl_team3.playlist.repository;

import com.example.sb10_MoPl_team3.playlist.entity.PlaylistContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlaylistContentRepository extends JpaRepository<PlaylistContent, UUID> {
    boolean existsByPlaylistIdAndContentId(UUID playlistId, UUID contentId);

    void deleteByPlaylistIdAndContentId(UUID playlistId, UUID contentId);

    List<PlaylistContent> findAllByPlaylistIdOrderByCreatedAtAsc(UUID playlistId);
}
