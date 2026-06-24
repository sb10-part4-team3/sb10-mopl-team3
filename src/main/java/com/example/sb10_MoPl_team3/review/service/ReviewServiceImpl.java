package com.example.sb10_MoPl_team3.review.service;

import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.repository.ContentRepository;
import com.example.sb10_MoPl_team3.domain.user.entity.User;
import com.example.sb10_MoPl_team3.domain.user.repository.UserRepository;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.review.dto.ReviewDto;
import com.example.sb10_MoPl_team3.review.dto.request.ReviewCreateRequest;
import com.example.sb10_MoPl_team3.review.entity.Review;
import com.example.sb10_MoPl_team3.review.enums.ReviewStatus;
import com.example.sb10_MoPl_team3.review.mapper.ReviewMapper;
import com.example.sb10_MoPl_team3.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIAL);
        }

        String email = authentication.getName();

        User author = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

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

    @Override
    public void delete(UUID id, UUID requestUserId) {
        Review targetReview = reviewRepository.findById(id)
                .orElseThrow();

    }

    @Override
    public void hardDelete(UUID id, UUID requestUserId) {

    }
}
