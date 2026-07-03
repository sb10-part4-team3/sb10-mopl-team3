package com.example.sb10_MoPl_team3.follow.repository;

import com.example.sb10_MoPl_team3.follow.entity.Follow;
import com.example.sb10_MoPl_team3.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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

    long countByFollowee_Id(UUID followeeId);

    long countByFollower(User follower); // 팔로잉 수

    @Query("select follow.follower.id from Follow follow where follow.followee.id = :followeeId")
    List<UUID> findFollowerIdsByFolloweeId(UUID followeeId);
}
