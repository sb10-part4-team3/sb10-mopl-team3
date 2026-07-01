package com.example.sb10_MoPl_team3.follow.mapper;

import com.example.sb10_MoPl_team3.follow.dto.FollowDto;
import com.example.sb10_MoPl_team3.follow.entity.Follow;
import com.example.sb10_MoPl_team3.user.entity.User;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-01T11:26:32+0900",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.17 (Azul Systems, Inc.)"
)
@Component
public class FollowMapperImpl implements FollowMapper {

    @Override
    public FollowDto toDto(Follow follow) {
        if ( follow == null ) {
            return null;
        }

        UUID followeeId = null;
        UUID followerId = null;
        UUID id = null;

        followeeId = followFolloweeId( follow );
        followerId = followFollowerId( follow );
        id = follow.getId();

        FollowDto followDto = new FollowDto( id, followeeId, followerId );

        return followDto;
    }

    private UUID followFolloweeId(Follow follow) {
        User followee = follow.getFollowee();
        if ( followee == null ) {
            return null;
        }
        return followee.getId();
    }

    private UUID followFollowerId(Follow follow) {
        User follower = follow.getFollower();
        if ( follower == null ) {
            return null;
        }
        return follower.getId();
    }
}
