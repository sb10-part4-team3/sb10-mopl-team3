package com.example.sb10_MoPl_team3.playlist.entity;

import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.global.base.BaseEntity;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Table(
        name = "playlist_contents",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_playlist_content",
                columnNames = {"playlist_id", "content_id"}
        )
)
public class PlaylistContent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id", nullable = false)
    private Playlist playlist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    public PlaylistContent(Playlist playlist, Content content) {
        this.playlist = playlist;
        this.content = content;
    }
}
