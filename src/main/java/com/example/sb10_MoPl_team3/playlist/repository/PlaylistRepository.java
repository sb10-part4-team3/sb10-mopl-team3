package com.example.sb10_MoPl_team3.playlist.repository;

import com.example.sb10_MoPl_team3.playlist.entity.Playlist;
import com.example.sb10_MoPl_team3.playlist.entity.PlaylistSubscriber;
import com.example.sb10_MoPl_team3.playlist.enums.PlaylistStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, UUID> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Playlist p
               set p.subscriberCount = p.subscriberCount + 1
             where p.id = :playlistId
               and p.status <> :deletedStatus
            """)
    int increaseSubscriberCount(UUID playlistId, PlaylistStatus deletedStatus);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Playlist p
               set p.subscriberCount =
                   case
                       when p.subscriberCount > 0 then p.subscriberCount - 1
                       else 0
                   end
             where p.id = :playlistId
               and p.status <> :deletedStatus
            """)
    int decreaseSubscriberCount(UUID playlistId, PlaylistStatus deletedStatus);
}
