package com.example.sb10_MoPl_team3.global.security;

import com.example.sb10_MoPl_team3.global.security.exception.AccessDeniedBusinessException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserAuthorizationService {

    public boolean isSelf(UUID userId) {
        return SecurityUtils.isCurrentUser(userId);
    }

    public void validateSelf(UUID userId) {
        if (!isSelf(userId)) {
            throw new AccessDeniedBusinessException();
        }
    }
}