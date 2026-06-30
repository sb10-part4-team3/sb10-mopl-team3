package com.example.sb10_MoPl_team3.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.repository.ContentRepository;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.global.security.AuthUser;
import com.example.sb10_MoPl_team3.review.dto.request.ReviewCreateRequest;
import com.example.sb10_MoPl_team3.review.dto.request.ReviewFindAllRequest;
import com.example.sb10_MoPl_team3.review.dto.request.ReviewUpdateRequest;
import com.example.sb10_MoPl_team3.review.dto.response.ReviewDto;
import com.example.sb10_MoPl_team3.review.entity.Review;
import com.example.sb10_MoPl_team3.review.enums.ReviewStatus;
import com.example.sb10_MoPl_team3.review.exception.ReviewAuthorMismatchException;
import com.example.sb10_MoPl_team3.review.exception.ReviewNotFoundException;
import com.example.sb10_MoPl_team3.review.mapper.ReviewMapper;
import com.example.sb10_MoPl_team3.review.repository.ReviewRepository;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContentRepository contentRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    @AfterEach
    void tearDown() {
        // SecurityUtils가 정적 SecurityContextHolder를 읽기 때문에 테스트 간 인증 상태를 격리한다.
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("create saves active review by authenticated user")
    void create_success() {
        UUID userId = uuid(1);
        UUID contentId = uuid(2);
        User author = user(userId, "author@test.com", "author");
        Content content = content(contentId);
        Review saved = review(uuid(3), content, author, "great", 4.5, ReviewStatus.ACTIVE);
        ReviewDto dto = reviewDto(saved);

        authenticate(userId);
        given(userRepository.findById(userId)).willReturn(Optional.of(author));
        given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
        given(reviewRepository.save(any(Review.class))).willReturn(saved);
        given(reviewMapper.toDto(saved)).willReturn(dto);

        ReviewDto response = reviewService.create(new ReviewCreateRequest(contentId, "great", 4.5));

        assertThat(response).isEqualTo(dto);
        // 저장 전 엔티티 상태를 캡처해서 인증 사용자, 콘텐츠, ACTIVE 상태가 실제로 조립됐는지 검증한다.
        ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
        then(reviewRepository).should().save(reviewCaptor.capture());
        Review captured = reviewCaptor.getValue();
        assertThat(captured.getAuthor()).isEqualTo(author);
        assertThat(captured.getContent()).isEqualTo(content);
        assertThat(captured.getText()).isEqualTo("great");
        assertThat(captured.getRating()).isEqualTo(4.5);
        assertThat(captured.getStatus()).isEqualTo(ReviewStatus.ACTIVE);
    }

    @Test
    @DisplayName("update changes own review")
    void update_success() {
        UUID userId = uuid(1);
        UUID reviewId = uuid(10);
        Review review = review(reviewId, content(uuid(2)), user(userId, "me@test.com", "me"),
                "before", 3.0, ReviewStatus.ACTIVE);
        ReviewDto dto = new ReviewDto(reviewId, review.getContent().getId(), null, "after", 5.0);

        authenticate(userId);
        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
        given(reviewMapper.toDto(review)).willReturn(dto);

        ReviewDto response = reviewService.update(reviewId, new ReviewUpdateRequest("after", 5.0));

        assertThat(response).isEqualTo(dto);
        assertThat(review.getText()).isEqualTo("after");
        assertThat(review.getRating()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("update rejects non-author")
    void update_authorMismatch() {
        UUID authorId = uuid(1);
        UUID requestUserId = uuid(2);
        UUID reviewId = uuid(10);
        Review review = review(reviewId, content(uuid(3)), user(authorId, "author@test.com", "author"),
                "text", 4.0, ReviewStatus.ACTIVE);

        authenticate(requestUserId);
        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.update(reviewId, new ReviewUpdateRequest("after", 5.0)))
                .isInstanceOfSatisfying(ReviewAuthorMismatchException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED)
                );
        assertThat(review.getText()).isEqualTo("text");
        assertThat(review.getRating()).isEqualTo(4.0);
        then(reviewMapper).should(never()).toDto(any());
    }

    @Test
    @DisplayName("findAll returns cursor page")
    void findAll_success() {
        UUID contentId = uuid(2);
        Review first = review(uuid(11), content(contentId), user(uuid(1), "a@test.com", "a"),
                "first", 4.0, ReviewStatus.ACTIVE);
        Review second = review(uuid(12), content(contentId), user(uuid(3), "b@test.com", "b"),
                "second", 3.0, ReviewStatus.ACTIVE);
        Review extra = review(uuid(13), content(contentId), user(uuid(4), "c@test.com", "c"),
                "extra", 2.0, ReviewStatus.ACTIVE);
        setAudit(first, "2026-06-29T00:00:00Z");
        setAudit(second, "2026-06-29T00:01:00Z");
        setAudit(extra, "2026-06-29T00:02:00Z");

        ReviewFindAllRequest request = new ReviewFindAllRequest(
                contentId,
                null,
                null,
                2,
                "ASCENDING",
                "createdAt"
        );
        // 서비스는 다음 페이지 존재 여부를 판단하려고 limit보다 1개 더 조회한다.
        given(reviewRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(first, second, extra)));
        given(reviewRepository.countByContent_IdAndStatus(contentId, ReviewStatus.ACTIVE))
                .willReturn(3L);
        given(reviewMapper.toDto(first)).willReturn(reviewDto(first));
        given(reviewMapper.toDto(second)).willReturn(reviewDto(second));

        var response = reviewService.findAll(request);

        assertThat(response.data()).hasSize(2);
        assertThat(response.hasNext()).isTrue();
        // extra는 응답에서 제외되고, 현재 페이지의 마지막 리뷰가 다음 커서가 된다.
        assertThat(response.nextCursor()).isEqualTo("2026-06-29T00:01:00Z");
        assertThat(response.nextIdAfter()).isEqualTo(second.getId());
        assertThat(response.totalCount()).isEqualTo(3L);
        assertThat(response.sortBy()).isEqualTo("createdAt");
        assertThat(response.sortDirection()).isEqualTo("ASCENDING");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        then(reviewRepository).should().findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(3);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt").isAscending()).isTrue();
    }

    @Test
    @DisplayName("findAll rejects cursor without idAfter")
    void findAll_invalidCursorPair() {
        ReviewFindAllRequest request = new ReviewFindAllRequest(
                uuid(2),
                "2026-06-29T00:00:00Z",
                null,
                20,
                "DESCENDING",
                "createdAt"
        );

        assertThatThrownBy(() -> reviewService.findAll(request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_CURSOR)
                );
    }

    @Test
    @DisplayName("delete soft-deletes own active review")
    void delete_success() {
        UUID userId = uuid(1);
        UUID reviewId = uuid(10);
        Review review = review(reviewId, content(uuid(2)), user(userId, "me@test.com", "me"),
                "text", 4.0, ReviewStatus.ACTIVE);

        authenticate(userId);
        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

        reviewService.delete(reviewId);

        assertThat(review.getStatus()).isEqualTo(ReviewStatus.DELETED);
        assertThat(review.getDeletedAt()).isNotNull();
        then(reviewRepository).should(never()).delete(any(Review.class));
    }

    @Test
    @DisplayName("hardDelete deletes own active review")
    void hardDelete_success() {
        UUID userId = uuid(1);
        UUID reviewId = uuid(10);
        Review review = review(reviewId, content(uuid(2)), user(userId, "me@test.com", "me"),
                "text", 4.0, ReviewStatus.ACTIVE);

        authenticate(userId);
        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

        reviewService.hardDelete(reviewId);

        then(reviewRepository).should().delete(review);
    }

    @Test
    @DisplayName("delete rejects already deleted review")
    void delete_deletedReview() {
        UUID userId = uuid(1);
        UUID reviewId = uuid(10);
        Review review = review(reviewId, content(uuid(2)), user(userId, "me@test.com", "me"),
                "text", 4.0, ReviewStatus.DELETED);

        authenticate(userId);
        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.delete(reviewId))
                .isInstanceOf(ReviewNotFoundException.class);
    }

    private void authenticate(UUID userId) {
        // 서비스가 SecurityUtils.getCurrentUserId()를 호출하므로 실제 인증 컨텍스트에 AuthUser를 심는다.
        AuthUser authUser = new AuthUser(userId, UserRole.USER, uuid(99));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(authUser, null, authUser.authorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private User user(UUID id, String email, String name) {
        User user = new User(email, name, "password", null, UserRole.USER);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Content content(UUID id) {
        Content content = Content.builder()
                .type(ContentType.MOVIE)
                .title("content")
                .description("description")
                .thumbnailUrl("thumbnail")
                .externalId("external-" + id)
                .source("tmdb")
                .build();
        ReflectionTestUtils.setField(content, "id", id);
        return content;
    }

    private Review review(
            UUID id,
            Content content,
            User author,
            String text,
            Double rating,
            ReviewStatus status
    ) {
        Review review = Review.builder()
                .content(content)
                .author(author)
                .text(text)
                .rating(rating)
                .status(status)
                .build();
        ReflectionTestUtils.setField(review, "id", id);
        return review;
    }

    private ReviewDto reviewDto(Review review) {
        return new ReviewDto(
                review.getId(),
                review.getContent().getId(),
                null,
                review.getText(),
                review.getRating()
        );
    }

    private void setAudit(Review review, String createdAt) {
        // BaseEntity 감사 필드는 JPA가 채우는 값이라 단위 테스트에서는 명시적으로 고정한다.
        ReflectionTestUtils.setField(review, "createdAt", Instant.parse(createdAt));
        ReflectionTestUtils.setField(review, "updatedAt", Instant.parse(createdAt));
    }

    private UUID uuid(int value) {
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", value));
    }
}
