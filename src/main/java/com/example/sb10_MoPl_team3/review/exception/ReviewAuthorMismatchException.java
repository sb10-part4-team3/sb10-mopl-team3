package com.example.sb10_MoPl_team3.review.exception;

import com.example.sb10_MoPl_team3.global.enums.ErrorCode;

import java.util.UUID;

public class ReviewAuthorMismatchException extends ReviewException {
    public ReviewAuthorMismatchException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ReviewAuthorMismatchException(UUID userId, UUID reviewId) {
        super(ErrorCode.ACCESS_DENIED);
    }
}
