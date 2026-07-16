package com.example.sb10_MoPl_team3.oauth.service;

import com.example.sb10_MoPl_team3.auth.dto.AuthTokenResult;
import com.example.sb10_MoPl_team3.auth.service.AuthService;
import com.example.sb10_MoPl_team3.oauth.dto.OAuthUserInfo;
import com.example.sb10_MoPl_team3.oauth.entity.SocialAccount;
import com.example.sb10_MoPl_team3.oauth.repository.SocialAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OAuthAuthenticationService {

    private final SocialAccountRepository socialAccountRepository;
    private final OAuthSocialAccountCreator socialAccountCreator;
    private final AuthService authService;

    public AuthTokenResult signin(OAuthUserInfo userInfo) {
        SocialAccount socialAccount = socialAccountRepository
                .findByProviderAndProviderUserId(
                        userInfo.provider(),
                        userInfo.providerUserId()
                )
                .orElseGet(() -> createOrFindSocialAccount(userInfo));

        return authService.issueTokenForAuthenticatedUser(socialAccount.getUser());
    }

    private SocialAccount createOrFindSocialAccount(OAuthUserInfo userInfo) {
        try {
            return socialAccountCreator.create(userInfo);
        } catch (DataIntegrityViolationException exception) {
            return socialAccountRepository
                    .findByProviderAndProviderUserId(
                            userInfo.provider(),
                            userInfo.providerUserId()
                    )
                    .orElseThrow(() -> exception);
        }
    }
}
