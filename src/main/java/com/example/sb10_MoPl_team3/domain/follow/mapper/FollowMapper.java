package com.example.sb10_MoPl_team3.domain.follow.mapper;

import com.example.sb10_MoPl_team3.domain.follow.dto.FollowDto;
import com.example.sb10_MoPl_team3.domain.follow.entity.Follow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FollowMapper {

    @Mapping(source = "followee.id", target = "followeeId")
    @Mapping(source = "follower.id", target = "followerId")
    FollowDto toDto(Follow follow);
}
