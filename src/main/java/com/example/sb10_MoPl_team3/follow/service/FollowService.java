package com.example.sb10_MoPl_team3.follow.service;

import com.example.sb10_MoPl_team3.follow.dto.FollowDto;
import com.example.sb10_MoPl_team3.follow.dto.FollowRequest;
import java.util.UUID;

public interface FollowService {

    FollowCreateResult create(UUID followerId, FollowRequest request);

    long getFollowerCount(UUID followeeId);

    FollowDto isFollowedByMe(UUID followerId, UUID followeeId);

    void cancel(UUID followerId, UUID followId);
}
