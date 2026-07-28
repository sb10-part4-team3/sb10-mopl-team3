package com.example.sb10_MoPl_team3.review.service;

import com.example.sb10_MoPl_team3.review.dto.request.ReviewFindAllRequest;
import com.example.sb10_MoPl_team3.review.dto.response.CursorResponseReviewDto;
import com.example.sb10_MoPl_team3.review.dto.response.ReviewDto;
import com.example.sb10_MoPl_team3.review.dto.request.ReviewCreateRequest;
import com.example.sb10_MoPl_team3.review.dto.request.ReviewUpdateRequest;

import java.util.UUID;

public interface ReviewService {
    // 리뷰 등록
    ReviewDto create(ReviewCreateRequest request);

    // 리뷰 수정
    ReviewDto update(UUID reviewId, ReviewUpdateRequest request);

    // 리뷰 목록 조회
    CursorResponseReviewDto<ReviewDto> findAll(ReviewFindAllRequest request);

    // 리뷰 논리 삭제
    void delete(UUID reviewId);

    // 리뷰 물리 삭제
    void hardDelete(UUID reviewId);
}
