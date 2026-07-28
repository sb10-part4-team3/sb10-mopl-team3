package com.example.sb10_MoPl_team3.review.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.sb10_MoPl_team3.global.config.SecurityConfig;
import com.example.sb10_MoPl_team3.global.exception.GlobalExceptionHandler;
import com.example.sb10_MoPl_team3.global.security.AuthUser;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtProvider;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtSessionValidator;
import com.example.sb10_MoPl_team3.review.dto.request.ReviewCreateRequest;
import com.example.sb10_MoPl_team3.review.dto.request.ReviewFindAllRequest;
import com.example.sb10_MoPl_team3.review.dto.request.ReviewUpdateRequest;
import com.example.sb10_MoPl_team3.review.dto.response.CursorResponseReviewDto;
import com.example.sb10_MoPl_team3.review.dto.response.ReviewDto;
import com.example.sb10_MoPl_team3.review.service.ReviewService;
import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReviewController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private JwtSessionValidator jwtSessionValidator;

    @Test
    @DisplayName("POST /api/reviews creates review and returns 201")
    void createReview_success() throws Exception {
        UUID contentId = uuid(1);
        UUID reviewId = uuid(2);
        ReviewDto response = reviewDto(reviewId, contentId, "great", 4.5);
        given(reviewService.create(any(ReviewCreateRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/reviews")
                        .with(csrf())
                        .with(authentication(authToken(uuid(10))))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "contentId": "%s",
                                  "text": "great",
                                  "rating": 4.5
                                }
                                """.formatted(contentId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(reviewId.toString()))
                .andExpect(jsonPath("$.contentId").value(contentId.toString()))
                .andExpect(jsonPath("$.text").value("great"))
                .andExpect(jsonPath("$.rating").value(4.5));

        ArgumentCaptor<ReviewCreateRequest> captor =
                ArgumentCaptor.forClass(ReviewCreateRequest.class);
        then(reviewService).should().create(captor.capture());

        ReviewCreateRequest capturedRequest = captor.getValue();
        assertThat(capturedRequest.contentId()).isEqualTo(contentId);
        assertThat(capturedRequest.text()).isEqualTo("great");
        assertThat(capturedRequest.rating()).isEqualTo(4.5);
    }

    @Test
    @DisplayName("PATCH /api/reviews/{reviewId} updates review and returns 200")
    void updateReview_success() throws Exception {
        UUID contentId = uuid(1);
        UUID reviewId = uuid(2);
        ReviewDto response = reviewDto(reviewId, contentId, "updated", 5.0);
        given(reviewService.update(eq(reviewId), any(ReviewUpdateRequest.class))).willReturn(response);

        mockMvc.perform(patch("/api/reviews/{reviewId}", reviewId)
                        .with(csrf())
                        .with(authentication(authToken(uuid(10))))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "text": "updated",
                                  "rating": 5.0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reviewId.toString()))
                .andExpect(jsonPath("$.text").value("updated"))
                .andExpect(jsonPath("$.rating").value(5.0));

        ArgumentCaptor<ReviewUpdateRequest> captor =
                ArgumentCaptor.forClass(ReviewUpdateRequest.class);
        then(reviewService).should().update(eq(reviewId), captor.capture());

        ReviewUpdateRequest capturedRequest = captor.getValue();
        assertThat(capturedRequest.text()).isEqualTo("updated");
        assertThat(capturedRequest.rating()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("GET /api/reviews binds query parameters to request DTO")
    void findAllReviews_bindsQueryParameters() throws Exception {
        UUID contentId = uuid(1);
        UUID idAfter = uuid(99);
        CursorResponseReviewDto<ReviewDto> response =
                new CursorResponseReviewDto<>(
                        List.of(),
                        null,
                        null,
                        false,
                        0L,
                        "createdAt",
                        "DESCENDING");
        given(reviewService.findAll(any(ReviewFindAllRequest.class))).willReturn(response);

        mockMvc.perform(get("/api/reviews")
                        .with(authentication(authToken(uuid(10))))
                        .param("contentId", contentId.toString())
                        .param("cursor", "2026-06-29T00:00:00Z")
                        .param("idAfter", idAfter.toString())
                        .param("limit", "20")
                        .param("sortDirection", "DESCENDING")
                        .param("sortBy", "createdAt"))
                .andExpect(status().isOk());

        ArgumentCaptor<ReviewFindAllRequest> captor =
                ArgumentCaptor.forClass(ReviewFindAllRequest.class);
        then(reviewService).should().findAll(captor.capture());

        ReviewFindAllRequest capturedRequest = captor.getValue();
        assertThat(capturedRequest.contentId()).isEqualTo(contentId);
        assertThat(capturedRequest.cursor()).isEqualTo("2026-06-29T00:00:00Z");
        assertThat(capturedRequest.idAfter()).isEqualTo(idAfter);
        assertThat(capturedRequest.limit()).isEqualTo(20);
        assertThat(capturedRequest.sortDirection()).isEqualTo("DESCENDING");
        assertThat(capturedRequest.sortBy()).isEqualTo("createdAt");
    }

    @Test
    @DisplayName("GET /api/reviews returns cursor response")
    void findAllReviews_success() throws Exception {
        UUID contentId = uuid(1);
        UUID reviewId = uuid(2);
        UUID nextIdAfter = uuid(3);
        ReviewDto review = reviewDto(reviewId, contentId, "great", 4.5);
        CursorResponseReviewDto<ReviewDto> response =
                new CursorResponseReviewDto<>(
                        List.of(review),
                        "2026-06-29T00:00:00Z",
                        nextIdAfter,
                        true,
                        2L,
                        "createdAt",
                        "DESCENDING");
        given(reviewService.findAll(any(ReviewFindAllRequest.class))).willReturn(response);

        mockMvc.perform(get("/api/reviews")
                        .with(authentication(authToken(uuid(10))))
                        .param("contentId", contentId.toString())
                        .param("limit", "1")
                        .param("sortDirection", "DESCENDING")
                        .param("sortBy", "createdAt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(reviewId.toString()))
                .andExpect(jsonPath("$.data[0].contentId").value(contentId.toString()))
                .andExpect(jsonPath("$.data[0].text").value("great"))
                .andExpect(jsonPath("$.data[0].rating").value(4.5))
                .andExpect(jsonPath("$.nextCursor").value("2026-06-29T00:00:00Z"))
                .andExpect(jsonPath("$.nextIdAfter").value(nextIdAfter.toString()))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.sortBy").value("createdAt"))
                .andExpect(jsonPath("$.sortDirection").value("DESCENDING"));
    }

    @Test
    @DisplayName("DELETE /api/reviews/{reviewId} deletes review and returns 200")
    void deleteReview_success() throws Exception {
        UUID reviewId = uuid(2);

        mockMvc.perform(delete("/api/reviews/{reviewId}", reviewId)
                        .with(csrf())
                        .with(authentication(authToken(uuid(10)))))
                .andExpect(status().isOk());

        then(reviewService).should().delete(reviewId);
    }

    private ReviewDto reviewDto(UUID reviewId, UUID contentId, String text, double rating) {
        UserSummary author = new UserSummary(uuid(10), "author", null);
        return new ReviewDto(reviewId, contentId, author, text, rating);
    }

    private UsernamePasswordAuthenticationToken authToken(UUID userId) {
        AuthUser authUser = new AuthUser(userId, UserRole.USER, UUID.randomUUID());
        return new UsernamePasswordAuthenticationToken(
                authUser, null, authUser.authorities());
    }

    private UUID uuid(int value) {
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", value));
    }
}
