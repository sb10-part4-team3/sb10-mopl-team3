package com.example.sb10_MoPl_team3.global.security.exception;

import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;

public class AccessDeniedBusinessException extends BusinessException {

    public AccessDeniedBusinessException() {
        super(ErrorCode.ACCESS_DENIED);
    }
}