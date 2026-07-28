package com.example.sb10_MoPl_team3.auth.repository;

import com.example.sb10_MoPl_team3.auth.entity.AuthSession;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuthSessionRepository extends CrudRepository<AuthSession, UUID> {

    Optional<AuthSession> findByRefreshTokenHash(String refreshTokenHash);

    Iterable<AuthSession> findAllByUserId(UUID userId);
}