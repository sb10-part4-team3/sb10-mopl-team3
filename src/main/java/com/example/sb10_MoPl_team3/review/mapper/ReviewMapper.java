package com.example.sb10_MoPl_team3.review.mapper;

import com.example.sb10_MoPl_team3.review.dto.ReviewDto;
import com.example.sb10_MoPl_team3.review.entity.Review;
import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {
    @Mapping(source = "author", target = "author")
    @Mapping(source = "content.id", target = "contentId")
    ReviewDto toDto(Review review);

    default UserSummary mapAuthor(User author) {
        if (author == null) {
            return null;
        }

        return UserMapper.toSummary(author);
    }
}
