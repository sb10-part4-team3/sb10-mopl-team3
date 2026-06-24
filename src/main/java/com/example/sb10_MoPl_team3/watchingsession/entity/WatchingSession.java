package com.example.sb10_MoPl_team3.watchingsession.entity;

import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(name = "watching_sessions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WatchingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "watcher_id",
            nullable = false,
            unique = true
    )
    private User watcher;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "content_id",
            nullable = false
    )
    private Content content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public WatchingSession(User watcher, Content content) {
        this.watcher = Objects.requireNonNull(watcher, "watcher는 필수입니다.");
        this.content = Objects.requireNonNull(content, "content는 필수입니다.");
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
