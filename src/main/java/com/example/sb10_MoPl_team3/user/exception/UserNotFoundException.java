package com.example.sb10_MoPl_team3.user.exception;

import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;

import java.util.Map;
import java.util.UUID;

public class UserNotFoundException extends BusinessException {

    public UserNotFoundException(UUID userId) {
        super(ErrorCode.USER_NOT_FOUND, Map.of("userId", userId));
    }
}