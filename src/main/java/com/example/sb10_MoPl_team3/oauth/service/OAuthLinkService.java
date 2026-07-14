package com.example.sb10_MoPl_team3.oauth.service;

import com.example.sb10_MoPl_team3.global.security.exception.AccessDeniedBusinessException;
import com.example.sb10_MoPl_team3.oauth.dto.OAuthUserInfo;
import com.example.sb10_MoPl_team3.oauth.entity.SocialAccount;
import com.example.sb10_MoPl_team3.oauth.exception.OAuthAccountAlreadyLinkedException;
import com.example.sb10_MoPl_team3.oauth.exception.OAuthProviderAlreadyLinkedException;
import com.example.sb10_MoPl_team3.oauth.repository.SocialAccountRepository;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserStatus;
import com.example.sb10_MoPl_team3.user.exception.UserNotFoundException;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OAuthLinkService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;

    @Transactional
    public SocialAccount link(UUID userId, OAuthUserInfo userInfo) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedBusinessException();
        }

        if (socialAccountRepository.existsByProviderAndProviderUserId(
                userInfo.provider(),
                userInfo.providerUserId()
        )) {
            throw new OAuthAccountAlreadyLinkedException();
        }

        if (socialAccountRepository.existsByUser_IdAndProvider(userId, userInfo.provider())) {
            throw new OAuthProviderAlreadyLinkedException();
        }

        SocialAccount socialAccount = SocialAccount.create(
                user,
                userInfo.provider(),
                userInfo.providerUserId(),
                userInfo.email()
        );

        return socialAccountRepository.save(socialAccount);
    }
}