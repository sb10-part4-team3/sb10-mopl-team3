package com.example.sb10_MoPl_team3.review.controller;

import com.example.sb10_MoPl_team3.review.dto.request.ReviewCreateRequest;
import com.example.sb10_MoPl_team3.review.dto.request.ReviewFindAllRequest;
import com.example.sb10_MoPl_team3.review.dto.request.ReviewUpdateRequest;
import com.example.sb10_MoPl_team3.review.dto.response.CursorResponseReviewDto;
import com.example.sb10_MoPl_team3.review.dto.response.ReviewDto;
import com.example.sb10_MoPl_team3.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewService reviewService;

    // 리뷰 생성
    @PostMapping
    public ResponseEntity<ReviewDto> createReview(
            @Valid @RequestBody ReviewCreateRequest request
    ) {
        ReviewDto reviewDto = reviewService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewDto);
    }

    // 리뷰 수정
    @PatchMapping(value = "/{reviewId}")
    public ResponseEntity<ReviewDto> updateReview(
            @PathVariable("reviewId") UUID reviewId,
            @RequestBody ReviewUpdateRequest request
    ) {
        ReviewDto reviewDto = reviewService.update(reviewId, request);
        return ResponseEntity.status(HttpStatus.OK).body(reviewDto);
    }

    // 리뷰 목록 조회
    @GetMapping
    public ResponseEntity<CursorResponseReviewDto<ReviewDto>> findAllReviews(
        @Valid @RequestBody ReviewFindAllRequest request
    ) {
        CursorResponseReviewDto<ReviewDto> response = reviewService.findAll(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 리뷰 삭제
    @DeleteMapping(value = "/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable("reviewId") UUID reviewId
    ) {
        reviewService.delete(reviewId);
        return ResponseEntity.ok().build();
    }
}
