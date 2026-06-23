package com.example.sb10_MoPl_team3.review.service;

import com.example.sb10_MoPl_team3.review.dto.ReviewDto;
import com.example.sb10_MoPl_team3.review.dto.request.ReviewCreateRequest;

public interface ReviewService {
    ReviewDto create(ReviewCreateRequest request);
}
