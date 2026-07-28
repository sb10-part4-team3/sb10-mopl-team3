package com.example.sb10_MoPl_team3.review.controller;

import com.example.sb10_MoPl_team3.global.openapi.ApiErrorResponses;
import com.example.sb10_MoPl_team3.review.dto.request.ReviewCreateRequest;
import com.example.sb10_MoPl_team3.review.dto.request.ReviewFindAllRequest;
import com.example.sb10_MoPl_team3.review.dto.request.ReviewUpdateRequest;
import com.example.sb10_MoPl_team3.review.dto.response.CursorResponseReviewDto;
import com.example.sb10_MoPl_team3.review.dto.response.ReviewDto;
import com.example.sb10_MoPl_team3.review.service.ReviewService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
@Tag(name = "리뷰 관리", description = "콘텐츠 리뷰 API")
@SecurityRequirement(name = "BearerAuth")
public class ReviewController {
    private final ReviewService reviewService;

    // 리뷰 생성
    @PostMapping
    @Operation(summary = "리뷰 생성")
    @ApiErrorResponses.Common
    @ApiResponse(responseCode = "201", description = "생성 성공", content = @Content(schema = @Schema(implementation = ReviewDto.class)))
    public ResponseEntity<ReviewDto> createReview(
            @Valid @RequestBody ReviewCreateRequest request
    ) {
        ReviewDto reviewDto = reviewService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewDto);
    }

    // 리뷰 수정
    @PatchMapping(value = "/{reviewId}")
    @Operation(summary = "리뷰 수정")
    @ApiErrorResponses.Forbidden
    public ResponseEntity<ReviewDto> updateReview(
            @PathVariable("reviewId") UUID reviewId,
            @RequestBody ReviewUpdateRequest request
    ) {
        ReviewDto reviewDto = reviewService.update(reviewId, request);
        return ResponseEntity.status(HttpStatus.OK).body(reviewDto);
    }

    // 리뷰 목록 조회
    @GetMapping
    @Operation(summary = "리뷰 목록 조회 (커서 페이지네이션)")
    @ApiErrorResponses.Common
    public ResponseEntity<CursorResponseReviewDto<ReviewDto>> findAllReviews(
            @Valid @ModelAttribute ReviewFindAllRequest request
    ) {
        CursorResponseReviewDto<ReviewDto> response = reviewService.findAll(request);
        return ResponseEntity.ok(response);
    }

    // 리뷰 삭제
    @DeleteMapping(value = "/{reviewId}")
    @Operation(summary = "리뷰 삭제")
    @ApiErrorResponses.Forbidden
    public ResponseEntity<Void> deleteReview(
            @PathVariable("reviewId") UUID reviewId
    ) {
        reviewService.delete(reviewId);
        return ResponseEntity.ok().build();
    }
}
