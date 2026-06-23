package com.example.sb10_MoPl_team3.domain.review.mapper;

import com.example.sb10_MoPl_team3.domain.review.dto.ReviewDto;
import com.example.sb10_MoPl_team3.domain.review.entity.Review;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReviewMapper {
    ReviewDto toDto(Review review);
}
