package com.example.sb10_MoPl_team3.follow.repository;

import com.example.sb10_MoPl_team3.follow.entity.Follow;
import com.example.sb10_MoPl_team3.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FollowRepository extends JpaRepository<Follow, UUID> {
    boolean existsByFollowerAndFollowee(
            User follower,
            User followee
    );

    long countByFollowee(User followee); // 팔로워 수

    long countByFollower(User follower); // 팔로잉 수
}
