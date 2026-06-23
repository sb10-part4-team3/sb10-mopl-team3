package com.example.sb10_MoPl_team3.domain.playlist.entity;

import com.example.sb10_MoPl_team3.domain.user.entity.User;
import com.example.sb10_MoPl_team3.global.base.BaseEntity;
import com.example.sb10_MoPl_team3.domain.playlist.enums.PlaylistStatus;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "playlists")
public class Playlist extends BaseEntity {
    // 소유자
    @Column(nullable = false)
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

    // 삭제일시
    private Instant deletedAt;

    // 생성자
    @Builder
    public void Playlist(User owner, String title, String description, PlaylistStatus status) {
        this.owner = owner;
        this.title = title;
        this.description = description;
        this.status = status;
    }


    // 플레이리스트 수정
    public void update(String title, String description) {
        if (title != null)
            this.title = title;
        if (description != null)
            this.description = description;
    }
}
