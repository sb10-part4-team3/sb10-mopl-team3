package com.example.sb10_MoPl_team3.playlist.repository;

import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.playlist.entity.Playlist;
import com.example.sb10_MoPl_team3.playlist.entity.PlaylistContent;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PlaylistContentRepositoryCustom {

    int insertIfNotExists(Playlist playlist, Content content);

    List<PlaylistContent> findAllWithPlaylistAndContentByPlaylistIds(Collection<UUID> playlistIds);
}
