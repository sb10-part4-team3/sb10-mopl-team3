package com.example.sb10_MoPl_team3.conversation.entity;

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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "conversations",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id_1", "user_id_2"})
    }
)
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
    User u1 = Objects.requireNonNull(user1, "user1은 필수입니다.");
    User u2 = Objects.requireNonNull(user2, "user2는 필수입니다.");
    UUID id1 = Objects.requireNonNull(u1.getId(), "user1 id는 필수입니다.");
    UUID id2 = Objects.requireNonNull(u2.getId(), "user2 id는 필수입니다.");

    if (id1.equals(id2)) {
      throw new IllegalArgumentException("동일한 사용자로 대화를 생성할 수 없습니다.");
    }

    if (id1.compareTo(id2) < 0) {
      this.user1 = u1;
      this.user2 = u2;
    } else {
      this.user1 = u2;
      this.user2 = u1;
    }
  }

  @PrePersist
  void prePersist() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }
}
