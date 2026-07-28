package com.example.sb10_MoPl_team3.oauth.service;

import com.example.sb10_MoPl_team3.auth.exception.InvalidCredentialException;
import com.example.sb10_MoPl_team3.oauth.dto.OAuthUserInfo;
import com.example.sb10_MoPl_team3.oauth.entity.SocialAccount;
import com.example.sb10_MoPl_team3.oauth.repository.SocialAccountRepository;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OAuthSocialAccountCreator {

    private final SocialAccountRepository socialAccountRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SocialAccount create(OAuthUserInfo userInfo) {
        String email = userInfo.resolvedEmail();

        if (email == null || email.isBlank()) {
            throw new InvalidCredentialException();
        }

        if (userInfo.hasProviderEmail() && !userInfo.emailVerified()) {
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

        return socialAccountRepository.saveAndFlush(socialAccount);
    }

    private User createUser(OAuthUserInfo userInfo, String email) {
        User user = new User(
                email,
                userInfo.name(),
                passwordEncoder.encode(UUID.randomUUID().toString()),
                userInfo.profileImageUrl(),
                UserRole.USER
        );

        return userRepository.saveAndFlush(user);
    }
}
