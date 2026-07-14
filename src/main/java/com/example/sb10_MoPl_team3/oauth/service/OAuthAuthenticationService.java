package com.example.sb10_MoPl_team3.oauth.service;

import com.example.sb10_MoPl_team3.auth.dto.AuthTokenResult;
import com.example.sb10_MoPl_team3.auth.service.AuthService;
import com.example.sb10_MoPl_team3.oauth.dto.OAuthUserInfo;
import com.example.sb10_MoPl_team3.oauth.entity.SocialAccount;
import com.example.sb10_MoPl_team3.oauth.exception.OAuthAccountNotLinkedException;
import com.example.sb10_MoPl_team3.oauth.repository.SocialAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OAuthAuthenticationService {

    private final SocialAccountRepository socialAccountRepository;
    private final AuthService authService;

    @Transactional
    public AuthTokenResult signin(OAuthUserInfo userInfo) {
        SocialAccount socialAccount = socialAccountRepository
                .findByProviderAndProviderUserId(
                        userInfo.provider(),
                        userInfo.providerUserId()
                )
                .orElseThrow(OAuthAccountNotLinkedException::new);

        return authService.issueTokenForAuthenticatedUser(socialAccount.getUser());
    }
}