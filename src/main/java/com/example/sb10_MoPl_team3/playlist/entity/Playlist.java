package com.example.sb10_MoPl_team3.playlist.entity;

import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.global.base.BaseEntity;
import com.example.sb10_MoPl_team3.playlist.enums.PlaylistStatus;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "playlists")
public class Playlist extends BaseEntity {
    // 소유자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    // 제목
    @Column(nullable = false)
    private String title;

    // 설명
    @Column(nullable = false, length = 500)
    private String description;

    // 플레이리스트 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlaylistStatus status;

    // 구독자 수
    @Column(nullable = false)
    private Integer subscriberCount;

    // 삭제일시
    private Instant deletedAt;

    // 생성자
    @Builder
    public Playlist(User owner, String title, String description, PlaylistStatus status) {

        this.owner = owner;
        this.title = title;
        this.description = description;
        this.status = status;
        this.subscriberCount = 0;
    }


    // 플레이리스트 수정
    public void update(String title, String description) {
        if (title != null)
            this.title = title;
        if (description != null)
            this.description = description;
    }

    // 플레이리스트 논리 삭제
    public void delete() {
        this.status = PlaylistStatus.DELETED;
        this.deletedAt = Instant.now();
    }
}
