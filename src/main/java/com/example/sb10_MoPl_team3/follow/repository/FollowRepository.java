package com.example.sb10_MoPl_team3.follow.repository;

import com.example.sb10_MoPl_team3.follow.entity.Follow;
import com.example.sb10_MoPl_team3.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FollowRepository extends JpaRepository<Follow, UUID> {
    boolean existsByFollowerAndFollowee(
            User follower,
            User followee
    );

    Optional<Follow> findByFollower_IdAndFollowee_Id(
            UUID followerId,
            UUID followeeId
    );

    long countByFollowee(User followee); // 팔로워 수

    long countByFollower(User follower); // 팔로잉 수
}
