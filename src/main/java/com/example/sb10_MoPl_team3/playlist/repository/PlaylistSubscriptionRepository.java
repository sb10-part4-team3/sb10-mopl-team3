package com.example.sb10_MoPl_team3.playlist.repository;

import com.example.sb10_MoPl_team3.playlist.entity.PlaylistSubscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface PlaylistSubscriptionRepository extends JpaRepository<PlaylistSubscriber, UUID>,
        PlaylistSubscriptionRepositoryCustom {
    boolean existsByPlaylistIdAndUserId(UUID playlistId, UUID userId);

    Optional<PlaylistSubscriber> findByPlaylistIdAndUserId(UUID playlistId, UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from PlaylistSubscriber ps
             where ps.playlist.id = :playlistId
               and ps.user.id = :userId
            """)
    int deleteByPlaylistIdAndUserId(UUID playlistId, UUID userId);

    @Query("""
        select ps.playlist.id
          from PlaylistSubscriber ps
         where ps.user.id = :userId
           and ps.playlist.id in :playlistIds
        """)
    Set<UUID> findSubscribedPlaylistIds(UUID userId, Collection<UUID> playlistIds);
}
