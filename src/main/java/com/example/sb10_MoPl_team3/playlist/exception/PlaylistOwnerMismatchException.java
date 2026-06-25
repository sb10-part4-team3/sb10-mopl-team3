package com.example.sb10_MoPl_team3.playlist.exception;

import com.example.sb10_MoPl_team3.global.enums.ErrorCode;

import java.util.UUID;

public class PlaylistOwnerMismatchException extends PlaylistException {
    public PlaylistOwnerMismatchException(ErrorCode errorCode) {
        super(errorCode);
    }

    public PlaylistOwnerMismatchException(UUID userID ,UUID playlistId) {
        super(ErrorCode.ACCESS_DENIED);
    }
}
