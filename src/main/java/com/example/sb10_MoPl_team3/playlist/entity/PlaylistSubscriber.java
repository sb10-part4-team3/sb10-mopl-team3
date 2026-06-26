package com.example.sb10_MoPl_team3.playlist.entity;

import com.example.sb10_MoPl_team3.global.base.BaseEntity;
import com.example.sb10_MoPl_team3.user.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "playlist_subscribers",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_playlist_subscriber_user_playlist",
                    columnNames = {"user_id", "playlist_id"}
            )
        }
)
public class PlaylistSubscriber extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id", nullable = false)
    private Playlist playlist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 생성자
    @Builder
    public PlaylistSubscriber(Playlist playlist, User user) {
        this.playlist = playlist;
        this.user = user;
    }
}
