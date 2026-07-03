package com.example.sb10_MoPl_team3.review.mapper;

import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.review.dto.request.ReviewCreateRequest;
import com.example.sb10_MoPl_team3.review.dto.response.ReviewDto;
import com.example.sb10_MoPl_team3.review.entity.Review;
import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-01T11:26:32+0900",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.17 (Azul Systems, Inc.)"
)
@Component
public class ReviewMapperImpl implements ReviewMapper {

    @Override
    public ReviewDto toDto(Review review) {
        if ( review == null ) {
            return null;
        }

        UserSummary author = null;
        UUID contentId = null;
        UUID id = null;
        String text = null;
        double rating = 0.0d;

        author = mapAuthor( review.getAuthor() );
        contentId = reviewContentId( review );
        id = review.getId();
        text = review.getText();
        if ( review.getRating() != null ) {
            rating = review.getRating();
        }

        ReviewDto reviewDto = new ReviewDto( id, contentId, author, text, rating );

        return reviewDto;
    }

    @Override
    public Review toEntity(ReviewCreateRequest request) {
        if ( request == null ) {
            return null;
        }

        Review.ReviewBuilder review = Review.builder();

        review.text( request.text() );
        review.rating( request.rating() );

        return review.build();
    }

    private UUID reviewContentId(Review review) {
        Content content = review.getContent();
        if ( content == null ) {
            return null;
        }
        return content.getId();
    }
}
