package com.example.sb10_MoPl_team3.playlist.repository;

import com.example.sb10_MoPl_team3.playlist.dto.request.PlaylistFindAllRequest;
import com.example.sb10_MoPl_team3.playlist.entity.Playlist;

import java.util.List;

public interface PlaylistRepositoryCustom {
    PlaylistSearchResult search(PlaylistFindAllRequest request);

    record PlaylistSearchResult(List<Playlist> playlists, long totalCount) {
    }
}
