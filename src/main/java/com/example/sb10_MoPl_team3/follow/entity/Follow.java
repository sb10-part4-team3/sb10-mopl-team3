package com.example.sb10_MoPl_team3.follow.entity;

import com.example.sb10_MoPl_team3.global.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor
@Table(
        name = "follows",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_follower_followee",
                        columnNames = {"follower_id", "followee_id"}
                )
        }
)
public class Follow extends BaseEntity {
    // 팔로워 대상 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "followee_id", nullable = false)
    private User followee;

    // 팔로워 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower;


    // 생성자
    @Builder
    public Follow(User followee, User follower) {
        this.followeeId = followee;
        this.followerId = follower;
    }
}
