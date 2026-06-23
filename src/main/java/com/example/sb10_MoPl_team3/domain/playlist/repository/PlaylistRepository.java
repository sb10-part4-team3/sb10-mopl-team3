package com.example.sb10_MoPl_team3.domain.playlist.repository;

import com.example.sb10_MoPl_team3.domain.playlist.entity.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, UUID> {
}
