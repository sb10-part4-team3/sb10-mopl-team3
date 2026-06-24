package com.example.sb10_MoPl_team3.review.service;

import com.example.sb10_MoPl_team3.review.dto.ReviewDto;
import com.example.sb10_MoPl_team3.review.dto.request.ReviewCreateRequest;

import java.util.UUID;

public interface ReviewService {
    // 리뷰 등록
    ReviewDto create(ReviewCreateRequest request);

    // 리뷰 논리 삭제
    void delete(UUID id, UUID requestUserId);

    // 리뷰 물리 삭제
    void hardDelete(UUID id, UUID requestUserId);
}
