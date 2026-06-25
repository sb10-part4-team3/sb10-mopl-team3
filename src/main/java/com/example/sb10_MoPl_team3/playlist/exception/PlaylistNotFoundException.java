package com.example.sb10_MoPl_team3.playlist.exception;

import com.example.sb10_MoPl_team3.global.enums.ErrorCode;

import java.util.UUID;

public class PlaylistNotFoundException extends PlaylistException {
    public PlaylistNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    public PlaylistNotFoundException(UUID playlistId) {
        super(ErrorCode.PLAYLIST_NOT_FOUND);
    }
}
