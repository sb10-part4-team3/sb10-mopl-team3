package com.example.sb10_MoPl_team3.playlist.exception;

import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;

public class PlaylistException extends BusinessException {
    public PlaylistException(ErrorCode errorCode) {
        super(errorCode);
    }
}
