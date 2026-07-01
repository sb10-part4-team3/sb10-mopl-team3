package com.example.sb10_MoPl_team3.user.entity;

import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.enums.UserStatus;
import com.example.sb10_MoPl_team3.global.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 60)
    private String password;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    public User(String email, String name, String password,String profileImageUrl, UserRole role) {
        this.email = email;
        this.name = name;
        this.password = password;
        this.profileImageUrl = profileImageUrl;
        this.role = role;
        this.status = UserStatus.ACTIVE;
    }

    public void changeRole(UserRole role) {
        this.role = role;
    }

    public void changeStatus(UserStatus status) {
        this.status = status;
    }

    public void changePassword(String password) {
        this.password = password;
    }

    public void updateProfile(String name, String profileImageUrl) {
        if (name != null) {
            this.name = name;
        }

        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
    }
}
