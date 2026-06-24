package com.example.sb10_MoPl_team3.auth.service;

import com.example.sb10_MoPl_team3.global.security.jwt.JwtProvider;
import com.example.sb10_MoPl_team3.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtProvider jwtProvider;

    public String issueAccessToken(User user) {
        return jwtProvider.generateAccessToken(
                user.getId(),
                user.getRole(),
                null
        );
    }
}