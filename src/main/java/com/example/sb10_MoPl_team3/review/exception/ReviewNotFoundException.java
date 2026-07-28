package com.example.sb10_MoPl_team3.review.exception;

import com.example.sb10_MoPl_team3.global.enums.ErrorCode;

import java.util.UUID;

public class ReviewNotFoundException extends ReviewException {
    public ReviewNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ReviewNotFoundException(UUID reviewId) {
        super(ErrorCode.REVIEW_NOT_FOUND);
    }
}
