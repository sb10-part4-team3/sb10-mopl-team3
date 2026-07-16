package com.example.sb10_MoPl_team3.oauth.service;

import com.example.sb10_MoPl_team3.auth.dto.AuthTokenResult;
import com.example.sb10_MoPl_team3.auth.dto.response.JwtDto;
import com.example.sb10_MoPl_team3.auth.service.AuthService;
import com.example.sb10_MoPl_team3.oauth.dto.OAuthUserInfo;
import com.example.sb10_MoPl_team3.oauth.entity.SocialAccount;
import com.example.sb10_MoPl_team3.oauth.enums.OAuthProvider;
import com.example.sb10_MoPl_team3.oauth.repository.SocialAccountRepository;
import com.example.sb10_MoPl_team3.user.dto.response.UserDto;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OAuthAuthenticationServiceTest {

    @Mock
    private SocialAccountRepository socialAccountRepository;

    @Mock
    private OAuthSocialAccountCreator socialAccountCreator;

    @Mock
    private AuthService authService;

    @InjectMocks
    private OAuthAuthenticationService oauthAuthenticationService;

    @Test
    @DisplayName("Linked OAuth account signs in with existing user")
    void signinLinkedAccount() {
        User user = user("user@test.com", "test-user");
        SocialAccount socialAccount = socialAccount(user);
        OAuthUserInfo userInfo = googleUserInfo();
        AuthTokenResult tokenResult = tokenResult(user);

        given(socialAccountRepository.findByProviderAndProviderUserId(
                OAuthProvider.GOOGLE,
                "google-user-id"
        )).willReturn(Optional.of(socialAccount));
        given(authService.issueTokenForAuthenticatedUser(user)).willReturn(tokenResult);

        AuthTokenResult result = oauthAuthenticationService.signin(userInfo);

        assertThat(result).isSameAs(tokenResult);

        verify(authService).issueTokenForAuthenticatedUser(user);
        verify(socialAccountCreator, never()).create(userInfo);
    }

    @Test
    @DisplayName("Unlinked OAuth account creates social account and signs in")
    void signinUnlinkedAccount() {
        User user = user("user@test.com", "test-user");
        SocialAccount socialAccount = socialAccount(user);
        OAuthUserInfo userInfo = googleUserInfo();
        AuthTokenResult tokenResult = tokenResult(user);

        given(socialAccountRepository.findByProviderAndProviderUserId(
                OAuthProvider.GOOGLE,
                "google-user-id"
        )).willReturn(Optional.empty());
        given(socialAccountCreator.create(userInfo)).willReturn(socialAccount);
        given(authService.issueTokenForAuthenticatedUser(user)).willReturn(tokenResult);

        AuthTokenResult result = oauthAuthenticationService.signin(userInfo);

        assertThat(result).isSameAs(tokenResult);

        verify(socialAccountCreator).create(userInfo);
        verify(authService).issueTokenForAuthenticatedUser(user);
    }

    @Test
    @DisplayName("Concurrent OAuth account creation collision signs in with reloaded social account")
    void signinConcurrentCreationCollision() {
        User existingUser = user("user@test.com", "existing-user");
        SocialAccount existingSocialAccount = socialAccount(existingUser);
        OAuthUserInfo userInfo = googleUserInfo();
        AuthTokenResult tokenResult = tokenResult(existingUser);

        given(socialAccountRepository.findByProviderAndProviderUserId(
                OAuthProvider.GOOGLE,
                "google-user-id"
        )).willReturn(Optional.empty(), Optional.of(existingSocialAccount));
        given(socialAccountCreator.create(userInfo))
                .willThrow(new DataIntegrityViolationException("duplicated oauth account"));
        given(authService.issueTokenForAuthenticatedUser(existingUser)).willReturn(tokenResult);

        AuthTokenResult result = oauthAuthenticationService.signin(userInfo);

        assertThat(result).isSameAs(tokenResult);

        verify(socialAccountRepository, times(2)).findByProviderAndProviderUserId(
                OAuthProvider.GOOGLE,
                "google-user-id"
        );
        verify(authService).issueTokenForAuthenticatedUser(existingUser);
    }

    private User user(String email, String name) {
        return new User(
                email,
                name,
                "encoded-password",
                null,
                UserRole.USER
        );
    }

    private SocialAccount socialAccount(User user) {
        return SocialAccount.create(
                user,
                OAuthProvider.GOOGLE,
                "google-user-id",
                "user@test.com"
        );
    }

    private OAuthUserInfo googleUserInfo() {
        return new OAuthUserInfo(
                OAuthProvider.GOOGLE,
                "google-user-id",
                "user@test.com",
                true,
                "test-user",
                null
        );
    }

    private AuthTokenResult tokenResult(User user) {
        return new AuthTokenResult(
                new JwtDto(
                        new UserDto(
                                UUID.randomUUID(),
                                Instant.now(),
                                user.getEmail(),
                                user.getName(),
                                user.getProfileImageUrl(),
                                user.getRole(),
                                false
                        ),
                        "access-token"
                ),
                "refresh-token"
        );
    }
}
