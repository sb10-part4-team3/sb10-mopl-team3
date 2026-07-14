package com.example.sb10_MoPl_team3.oauth.service;

import com.example.sb10_MoPl_team3.auth.dto.AuthTokenResult;
import com.example.sb10_MoPl_team3.auth.exception.InvalidCredentialException;
import com.example.sb10_MoPl_team3.auth.service.AuthService;
import com.example.sb10_MoPl_team3.oauth.dto.OAuthUserInfo;
import com.example.sb10_MoPl_team3.oauth.entity.SocialAccount;
import com.example.sb10_MoPl_team3.oauth.repository.SocialAccountRepository;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OAuthAuthenticationService {

    private final SocialAccountRepository socialAccountRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    @Transactional
    public AuthTokenResult signin(OAuthUserInfo userInfo) {
        SocialAccount socialAccount = socialAccountRepository
                .findByProviderAndProviderUserId(
                        userInfo.provider(),
                        userInfo.providerUserId()
                )
                .orElseGet(() -> createSocialAccount(userInfo));

        return authService.issueTokenForAuthenticatedUser(socialAccount.getUser());
    }

    private SocialAccount createSocialAccount(OAuthUserInfo userInfo) {
        String email = userInfo.resolvedEmail();

        if (email == null || email.isBlank()) {
            throw new InvalidCredentialException();
        }

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createUser(userInfo, email));

        SocialAccount socialAccount = SocialAccount.create(
                user,
                userInfo.provider(),
                userInfo.providerUserId(),
                userInfo.email()
        );

        return socialAccountRepository.save(socialAccount);
    }

    private User createUser(OAuthUserInfo userInfo, String email) {
        User user = new User(
                email,
                userInfo.name(),
                passwordEncoder.encode(UUID.randomUUID().toString()),
                userInfo.profileImageUrl(),
                UserRole.USER
        );

        return userRepository.save(user);
    }
}