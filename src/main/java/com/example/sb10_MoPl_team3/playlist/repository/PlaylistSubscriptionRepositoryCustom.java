package com.example.sb10_MoPl_team3.playlist.repository;

import com.example.sb10_MoPl_team3.playlist.entity.Playlist;
import com.example.sb10_MoPl_team3.user.entity.User;

public interface PlaylistSubscriptionRepositoryCustom {

    int insertIfNotExists(Playlist playlist, User user);
}
