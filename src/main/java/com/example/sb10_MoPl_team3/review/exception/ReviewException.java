package com.example.sb10_MoPl_team3.review.exception;

import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;

public class ReviewException extends BusinessException {
    public ReviewException(ErrorCode errorCode) {
        super(errorCode);
    }
}
