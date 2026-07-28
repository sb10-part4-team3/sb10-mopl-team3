package com.example.sb10_MoPl_team3.review.mapper;

import com.example.sb10_MoPl_team3.review.dto.response.ReviewDto;
import com.example.sb10_MoPl_team3.review.dto.request.ReviewCreateRequest;
import com.example.sb10_MoPl_team3.review.entity.Review;
import com.example.sb10_MoPl_team3.user.mapper.UserResponseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = UserResponseMapper.class)
public interface ReviewMapper {
    @Mapping(source = "author", target = "author")
    @Mapping(source = "content.id", target = "contentId")
    ReviewDto toDto(Review review);

    Review toEntity(ReviewCreateRequest request);
}
