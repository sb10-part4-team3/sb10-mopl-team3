package com.example.sb10_MoPl_team3.review.service;

import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.repository.ContentRepository;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.global.security.SecurityUtils;
import com.example.sb10_MoPl_team3.review.dto.request.ReviewFindAllRequest;
import com.example.sb10_MoPl_team3.review.dto.response.CursorResponseReviewDto;
import com.example.sb10_MoPl_team3.review.dto.response.ReviewDto;
import com.example.sb10_MoPl_team3.review.dto.request.ReviewCreateRequest;
import com.example.sb10_MoPl_team3.review.dto.request.ReviewUpdateRequest;
import com.example.sb10_MoPl_team3.review.entity.Review;
import com.example.sb10_MoPl_team3.review.enums.ReviewStatus;
import com.example.sb10_MoPl_team3.review.exception.ReviewAuthorMismatchException;
import com.example.sb10_MoPl_team3.review.exception.ReviewNotFoundException;
import com.example.sb10_MoPl_team3.review.mapper.ReviewMapper;
import com.example.sb10_MoPl_team3.review.repository.ReviewRepository;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService{

    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;

    @Override
    public ReviewDto create(ReviewCreateRequest request) {
        UUID requestUserId = getAuthenticatedUserId();

        User author = userRepository.findById(requestUserId)
                .orElseThrow(() ->
                        // UserNotFoundException 추가되면 변경
                        new BusinessException(ErrorCode.USER_NOT_FOUND));

        Content content = contentRepository.findById(request.contentId())
                // CONTENT_NOT_FOUND 추가 예정
                .orElseThrow();

        Review newReview = Review.builder()
                .content(content)
                .author(author)
                .text(request.text())
                .rating(request.rating())
                .status(ReviewStatus.ACTIVE)
                .build();

        return reviewMapper.toDto(reviewRepository.save(newReview));
    }

    // 리뷰 수정
    @Override
    public ReviewDto update(UUID reviewId, ReviewUpdateRequest request) {
        UUID requestUserId = getAuthenticatedUserId();

        Review targetReview = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        if (!targetReview.getAuthor().getId().equals(requestUserId)) {
            throw new ReviewAuthorMismatchException(ErrorCode.ACCESS_DENIED);
        }

        targetReview.update(request.text(), request.rating());

        return reviewMapper.toDto(targetReview);
    }

    /**
     * 특정 콘텐츠의 활성 리뷰를 커서 기반으로 조회한다.
     * 정렬값이 같은 리뷰는 ID를 보조 커서로 사용하여 조회 순서를 보장한다.
     */
    @Override
    @Transactional(readOnly = true)
    public CursorResponseReviewDto<ReviewDto> findAll(
            ReviewFindAllRequest request
    ) {
        // 정렬 기준을 검증하고, 값이 없으면 생성일시를 기본값으로 사용한다.
        String sortBy;

        if (request.sortBy() == null || request.sortBy().isBlank()) {
            sortBy = "createdAt";
        } else if ("createdAt".equalsIgnoreCase(request.sortBy())) {
            sortBy = "createdAt";
        } else if ("rating".equalsIgnoreCase(request.sortBy())) {
            sortBy = "rating";
        } else {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 정렬 방향을 검증하고, 값이 없으면 내림차순을 기본값으로 사용한다.
        String sortDirection =
                request.sortDirection() == null
                        ? "DESCENDING"
                        : request.sortDirection().toUpperCase(Locale.ROOT);

        if (!sortDirection.equals("ASCENDING")
                && !sortDirection.equals("DESCENDING")) {
            throw new BusinessException(ErrorCode.INVALID_SORT_DIRECTION);
        }

        boolean ascending = sortDirection.equals("ASCENDING");

        // 페이지 크기를 기본 20개, 최대 100개로 제한한다.
        int limit = request.limit() <= 0
                ? 20
                : Math.min(request.limit(), 100);

        boolean hasCursor =
                request.cursor() != null && !request.cursor().isBlank();
        boolean hasIdAfter = request.idAfter() != null;

        // 정렬값 커서와 보조 커서 ID는 반드시 함께 전달되어야 한다.
        if (hasCursor != hasIdAfter) {
            throw new BusinessException(ErrorCode.INVALID_CURSOR);
        }

        // 정렬 기준에 맞춰 커서 문자열을 Instant 또는 Double로 변환한다.
        Object cursorValue = null;

        if (hasCursor) {
            try {
                if (sortBy.equals("createdAt")) {
                    cursorValue = Instant.parse(request.cursor());
                } else {
                    double parsedRating = Double.parseDouble(request.cursor());

                    if (!Double.isFinite(parsedRating)) {
                        throw new NumberFormatException();
                    }

                    cursorValue = parsedRating;
                }
            } catch (RuntimeException exception) {
                throw new BusinessException(ErrorCode.INVALID_CURSOR);
            }
        }

        Object finalCursorValue = cursorValue;

        // 콘텐츠, 리뷰 상태 및 커서 위치를 반영한 동적 조회 조건을 생성한다.
        Specification<Review> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 요청한 콘텐츠의 활성 리뷰만 조회한다.
            predicates.add(
                    cb.equal(root.get("content").get("id"), request.contentId())
            );
            predicates.add(
                    cb.equal(root.get("status"), ReviewStatus.ACTIVE)
            );

            if (finalCursorValue != null) {
                Path<UUID> idPath = root.get("id");

                // 정렬값이 같을 경우 ID를 비교하여 페이지 간 중복과 누락을 방지한다.
                Predicate idCondition = ascending
                        ? cb.greaterThan(idPath, request.idAfter())
                        : cb.lessThan(idPath, request.idAfter());

                Predicate cursorCondition;

                if (sortBy.equals("createdAt")) {
                    Path<Instant> sortPath = root.get("createdAt");
                    Instant cursor = (Instant) finalCursorValue;

                    Predicate valueCondition = ascending
                            ? cb.greaterThan(sortPath, cursor)
                            : cb.lessThan(sortPath, cursor);

                    cursorCondition = cb.or(
                            valueCondition,
                            cb.and(
                                    cb.equal(sortPath, cursor),
                                    idCondition
                            )
                    );
                } else {
                    Path<Double> sortPath = root.get("rating");
                    Double cursor = (Double) finalCursorValue;

                    Predicate valueCondition = ascending
                            ? cb.greaterThan(sortPath, cursor)
                            : cb.lessThan(sortPath, cursor);

                    cursorCondition = cb.or(
                            valueCondition,
                            cb.and(
                                    cb.equal(sortPath, cursor),
                                    idCondition
                            )
                    );
                }

                predicates.add(cursorCondition);
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };

        Sort.Direction direction = ascending
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        // 정렬값이 같은 리뷰의 순서는 ID를 보조 정렬 기준으로 고정한다.
        Sort sort = Sort.by(direction, sortBy)
                .and(Sort.by(direction, "id"));

        // 다음 페이지 존재 여부를 확인하기 위해 요청 크기보다 한 개 더 조회한다.
        List<Review> fetchedReviews = new ArrayList<>(
                reviewRepository.findAll(
                        specification,
                        PageRequest.of(0, limit + 1, sort)
                ).getContent()
        );

        boolean hasNext = fetchedReviews.size() > limit;

        List<Review> reviews = hasNext
                ? fetchedReviews.subList(0, limit)
                : fetchedReviews;

        String nextCursor = null;
        UUID nextIdAfter = null;

        // 다음 페이지가 있으면 현재 페이지의 마지막 리뷰로 다음 커서를 생성한다.
        if (hasNext && !reviews.isEmpty()) {
            Review lastReview = reviews.get(reviews.size() - 1);

            nextCursor = sortBy.equals("createdAt")
                    ? lastReview.getCreatedAt().toString()
                    : lastReview.getRating().toString();

            nextIdAfter = lastReview.getId();
        }

        List<ReviewDto> data = reviews.stream()
                .map(reviewMapper::toDto)
                .toList();

        // 커서 위치와 관계없이 해당 콘텐츠의 전체 활성 리뷰 수를 조회한다.
        long totalCount =
                reviewRepository.countByContent_IdAndStatus(
                        request.contentId(),
                        ReviewStatus.ACTIVE
                );

        return new CursorResponseReviewDto<>(
                data,
                nextCursor,
                nextIdAfter,
                hasNext,
                totalCount,
                sortBy,
                sortDirection
        );
    }

    // 리뷰 논리 삭제
    @Override
    public void delete(UUID reviewId) {
        UUID requestUserId = getAuthenticatedUserId();
        Review targetReview = getReviewOrThrow(reviewId);

        validateReviewStatus(targetReview);
        validateAuthor(targetReview, requestUserId);

        targetReview.delete();
    }

    // 리뷰 물리 삭제
    @Override
    public void hardDelete(UUID reviewId) {
        UUID requestUserId = getAuthenticatedUserId();
        Review targetReview = getReviewOrThrow(reviewId);

        validateReviewStatus(targetReview);
        validateAuthor(targetReview, requestUserId);

        reviewRepository.delete(targetReview);
    }

    // 권한 확인
    private void validateAuthor(Review review, UUID userId) {
        if (!review.getAuthor().getId().equals(userId)) {
            throw new ReviewAuthorMismatchException(userId, review.getId());
        }
    }


    // 인증 사용자 조회
    private UUID getAuthenticatedUserId() {
        return SecurityUtils.getCurrentUserId();
    }

    // 리뷰 조회 후 반환
    private Review getReviewOrThrow(UUID reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));
    }

    // 유효성 검증(리뷰 상태 검증)
    private void validateReviewStatus(Review review) {
        if (review.getStatus() == ReviewStatus.DELETED) {
            throw new ReviewNotFoundException(review.getId());
        }
    }
}
