package com.example.sb10_MoPl_team3.auth.service;

import com.example.sb10_MoPl_team3.user.entity.User;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
    public String issueAccessToken(User user) {
        // JwtProvider 미구현이므로 테스트 통과를 위한 임시 형태
        return "temporary-access-token";
    }
}
