package com.example.sb10_MoPl_team3.follow.service;

import com.example.sb10_MoPl_team3.follow.dto.FollowDto;
import com.example.sb10_MoPl_team3.follow.dto.FollowRequest;
import java.util.UUID;

public interface FollowService {

    FollowDto create(UUID followerId, FollowRequest request);

    void cancel(UUID followerId, UUID followId);
}
