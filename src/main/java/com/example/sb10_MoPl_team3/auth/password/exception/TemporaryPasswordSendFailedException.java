package com.example.sb10_MoPl_team3.auth.password.exception;

import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;

public class TemporaryPasswordSendFailedException extends BusinessException {

    public TemporaryPasswordSendFailedException(Throwable cause) {
        super(ErrorCode.TEMPORARY_PASSWORD_SEND_FAILED, cause);
    }
}