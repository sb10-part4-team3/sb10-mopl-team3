package com.example.sb10_MoPl_team3.directmessage.entity;

import com.example.sb10_MoPl_team3.conversation.entity.Conversation;
import com.example.sb10_MoPl_team3.global.base.BaseEntity;
import com.example.sb10_MoPl_team3.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "direct_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DirectMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "read_at")
    private Instant readAt;

    public DirectMessage(
        Conversation conversation,
        User sender,
        User receiver,
        String content
    ) {
        this.conversation = Objects.requireNonNull(conversation, "conversation은 필수입니다.");
        this.sender = Objects.requireNonNull(sender, "sender는 필수입니다.");
        this.receiver = Objects.requireNonNull(receiver, "receiver는 필수입니다.");
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content는 필수입니다.");
        }
        this.content = content;
        this.read = false;
    }

    public void markAsRead(Instant readAt) {
        if (read) {
            return;
        }
        this.read = true;
        this.readAt = Objects.requireNonNull(readAt, "readAt은 필수입니다.");
    }
}
