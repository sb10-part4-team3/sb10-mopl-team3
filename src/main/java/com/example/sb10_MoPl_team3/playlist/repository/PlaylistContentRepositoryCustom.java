package com.example.sb10_MoPl_team3.playlist.repository;

import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.playlist.entity.Playlist;

public interface PlaylistContentRepositoryCustom {

    int insertIfNotExists(Playlist playlist, Content content);
}
