package com.example.sb10_MoPl_team3.domain.review.mapper;

import com.example.sb10_MoPl_team3.domain.review.dto.ReviewDto;
import com.example.sb10_MoPl_team3.domain.review.entity.Review;
import com.example.sb10_MoPl_team3.domain.user.dto.response.UserSummaryResponse;
import com.example.sb10_MoPl_team3.domain.user.entity.User;
import com.example.sb10_MoPl_team3.domain.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {
    @Mapping(source = "author", target = "author")
    @Mapping(source = "content.id", target = "contentId")
    ReviewDto toDto(Review review);

    default UserSummaryResponse mapAuthor(User author) {
        if (author == null) {
            return null;
        }

        return UserMapper.toSummaryResponse(author);
    }
}
