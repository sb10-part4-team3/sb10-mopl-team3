package com.example.sb10_MoPl_team3.auth.password.repository;

import com.example.sb10_MoPl_team3.auth.password.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    List<PasswordResetToken> findAllByUser_IdAndUsedFalse(UUID userId);
}