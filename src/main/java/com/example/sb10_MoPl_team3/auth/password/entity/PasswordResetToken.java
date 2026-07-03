package com.example.sb10_MoPl_team3.auth.password.entity;

import com.example.sb10_MoPl_team3.global.base.BaseEntity;
import com.example.sb10_MoPl_team3.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(name = "password_reset_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "temporary_password", nullable = false)
    private String temporaryPassword;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used;

    @Column(name = "used_at")
    private Instant usedAt;

    private PasswordResetToken(
            User user,
            String temporaryPassword,
            Instant expiresAt,
            Instant createdAt
    ) {
        this.user = user;
        this.temporaryPassword = temporaryPassword;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.used = false;
        this.usedAt = null;
    }

    public static PasswordResetToken create(
            User user,
            String temporaryPassword,
            Instant expiresAt,
            Instant createdAt
    ) {
        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("expiresAt must be after createdAt");
        }

        return new PasswordResetToken(user, temporaryPassword, expiresAt, createdAt);
    }

    public void markUsed(Instant now) {
        if (used) {
            return;
        }

        this.used = true;
        this.usedAt = now;
    }

    public boolean isUsable(Instant now) {
        return !used && expiresAt.isAfter(now);
    }
}