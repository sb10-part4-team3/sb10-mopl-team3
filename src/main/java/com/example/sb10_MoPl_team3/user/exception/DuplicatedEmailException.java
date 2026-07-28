package com.example.sb10_MoPl_team3.user.exception;

import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;

public class DuplicatedEmailException extends BusinessException {
    public DuplicatedEmailException() {
        super(ErrorCode.DUPLICATE_EMAIL);
    }
}
