package com.example.sb10_MoPl_team3.conversation.entity;

import com.example.sb10_MoPl_team3.domain.user.entity.User;
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
@Table(name = "conversations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id_1", nullable = false)
    private User user1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id_2", nullable = false)
    private User user2;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Conversation(User user1, User user2) {
        this.user1 = Objects.requireNonNull(user1, "user1은 필수입니다.");
        this.user2 = Objects.requireNonNull(user2, "user2는 필수입니다.");

        UUID user1Id = user1.getId();
        UUID user2Id = user2.getId();
        if (user1 == user2 || user1Id != null && Objects.equals(user1Id, user2Id)) {
            throw new IllegalArgumentException("동일한 사용자로 대화를 생성할 수 없습니다.");
        }
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
