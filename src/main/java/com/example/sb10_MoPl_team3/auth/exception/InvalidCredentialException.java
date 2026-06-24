package com.example.sb10_MoPl_team3.auth.exception;

import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;

public class InvalidCredentialException extends BusinessException {
    public InvalidCredentialException() {
        super(ErrorCode.INVALID_CREDENTIAL);
    }
}
