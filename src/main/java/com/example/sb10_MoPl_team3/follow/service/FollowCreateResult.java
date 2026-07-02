package com.example.sb10_MoPl_team3.follow.service;

import com.example.sb10_MoPl_team3.follow.dto.FollowDto;

public record FollowCreateResult(
        FollowDto follow,
        boolean created
) {
}
