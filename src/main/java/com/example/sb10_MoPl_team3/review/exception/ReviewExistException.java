package com.example.sb10_MoPl_team3.review.exception;

import com.example.sb10_MoPl_team3.global.enums.ErrorCode;

import java.util.UUID;

public class ReviewExistException extends ReviewException {
    public ReviewExistException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ReviewExistException(UUID reviewId) {
        super(ErrorCode.DUPLICATE_REVIEW);
    }
}
