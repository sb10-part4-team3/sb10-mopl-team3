package com.example.sb10_MoPl_team3.oauth.service;

import com.example.sb10_MoPl_team3.auth.dto.AuthTokenResult;
import com.example.sb10_MoPl_team3.auth.dto.response.JwtDto;
import com.example.sb10_MoPl_team3.auth.exception.InvalidCredentialException;
import com.example.sb10_MoPl_team3.auth.service.AuthService;
import com.example.sb10_MoPl_team3.oauth.dto.OAuthUserInfo;
import com.example.sb10_MoPl_team3.oauth.entity.SocialAccount;
import com.example.sb10_MoPl_team3.oauth.enums.OAuthProvider;
import com.example.sb10_MoPl_team3.oauth.repository.SocialAccountRepository;
import com.example.sb10_MoPl_team3.user.dto.response.UserDto;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OAuthAuthenticationServiceTest {

    @Mock
    private SocialAccountRepository socialAccountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthService authService;

    @InjectMocks
    private OAuthAuthenticationService oauthAuthenticationService;

    @Test
    @DisplayName("Linked OAuth account signs in with existing user")
    void signinLinkedAccount() {
        User user = new User(
                "user@test.com",
                "test-user",
                "encoded-password",
                null,
                UserRole.USER
        );

        SocialAccount socialAccount = SocialAccount.create(
                user,
                OAuthProvider.GOOGLE,
                "google-user-id",
                "user@test.com"
        );

        OAuthUserInfo userInfo = new OAuthUserInfo(
                OAuthProvider.GOOGLE,
                "google-user-id",
                "user@test.com",
                true,
                "test-user",
                null
        );

        AuthTokenResult tokenResult = tokenResult(user);

        given(socialAccountRepository.findByProviderAndProviderUserId(
                OAuthProvider.GOOGLE,
                "google-user-id"
        )).willReturn(Optional.of(socialAccount));
        given(authService.issueTokenForAuthenticatedUser(user)).willReturn(tokenResult);

        AuthTokenResult result = oauthAuthenticationService.signin(userInfo);

        assertThat(result).isSameAs(tokenResult);

        verify(authService).issueTokenForAuthenticatedUser(user);
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Unlinked OAuth account creates user and social account")
    void signinUnlinkedAccount() {
        OAuthUserInfo userInfo = new OAuthUserInfo(
                OAuthProvider.KAKAO,
                "kakao-user-id",
                null,
                false,
                "kakao-user",
                "https://image.test/profile.png"
        );

        User savedUser = new User(
                "kakao-user_kakao-user-id@kakao.com",
                "kakao-user",
                "encoded-random-password",
                "https://image.test/profile.png",
                UserRole.USER
        );

        SocialAccount savedSocialAccount = SocialAccount.create(
                savedUser,
                OAuthProvider.KAKAO,
                "kakao-user-id",
                null
        );

        AuthTokenResult tokenResult = tokenResult(savedUser);

        given(socialAccountRepository.findByProviderAndProviderUserId(
                OAuthProvider.KAKAO,
                "kakao-user-id"
        )).willReturn(Optional.empty());
        given(userRepository.findByEmail("kakao-user_kakao-user-id@kakao.com"))
                .willReturn(Optional.empty());
        given(passwordEncoder.encode(any(String.class))).willReturn("encoded-random-password");
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(socialAccountRepository.save(any(SocialAccount.class))).willReturn(savedSocialAccount);
        given(authService.issueTokenForAuthenticatedUser(savedUser)).willReturn(tokenResult);

        AuthTokenResult result = oauthAuthenticationService.signin(userInfo);

        assertThat(result).isSameAs(tokenResult);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User createdUser = userCaptor.getValue();
        assertThat(createdUser.getEmail()).isEqualTo("kakao-user_kakao-user-id@kakao.com");
        assertThat(createdUser.getName()).isEqualTo("kakao-user");
        assertThat(createdUser.getProfileImageUrl()).isEqualTo("https://image.test/profile.png");
        assertThat(createdUser.getRole()).isEqualTo(UserRole.USER);

        ArgumentCaptor<SocialAccount> socialAccountCaptor = ArgumentCaptor.forClass(SocialAccount.class);
        verify(socialAccountRepository).save(socialAccountCaptor.capture());

        SocialAccount createdSocialAccount = socialAccountCaptor.getValue();
        assertThat(createdSocialAccount.getUser()).isSameAs(savedUser);
        assertThat(createdSocialAccount.getProvider()).isEqualTo(OAuthProvider.KAKAO);
        assertThat(createdSocialAccount.getProviderUserId()).isEqualTo("kakao-user-id");
        assertThat(createdSocialAccount.getProviderEmail()).isNull();

        verify(authService).issueTokenForAuthenticatedUser(savedUser);
    }

    @Test
    @DisplayName("Unlinked OAuth account links existing email user")
    void signinUnlinkedAccountWithExistingEmailUser() {
        OAuthUserInfo userInfo = new OAuthUserInfo(
                OAuthProvider.GOOGLE,
                "google-user-id",
                "user@test.com",
                true,
                "google-user",
                null
        );

        User existingUser = new User(
                "user@test.com",
                "existing-user",
                "encoded-password",
                null,
                UserRole.USER
        );

        SocialAccount savedSocialAccount = SocialAccount.create(
                existingUser,
                OAuthProvider.GOOGLE,
                "google-user-id",
                "user@test.com"
        );

        AuthTokenResult tokenResult = tokenResult(existingUser);

        given(socialAccountRepository.findByProviderAndProviderUserId(
                OAuthProvider.GOOGLE,
                "google-user-id"
        )).willReturn(Optional.empty());
        given(userRepository.findByEmail("user@test.com")).willReturn(Optional.of(existingUser));
        given(socialAccountRepository.save(any(SocialAccount.class))).willReturn(savedSocialAccount);
        given(authService.issueTokenForAuthenticatedUser(existingUser)).willReturn(tokenResult);

        AuthTokenResult result = oauthAuthenticationService.signin(userInfo);

        assertThat(result).isSameAs(tokenResult);

        verify(userRepository).findByEmail("user@test.com");
        verify(socialAccountRepository).save(any(SocialAccount.class));
        verify(authService).issueTokenForAuthenticatedUser(existingUser);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("Unverified provider email cannot be used to link an existing user")
    void signinUnverifiedProviderEmail() {
        OAuthUserInfo userInfo = new OAuthUserInfo(
                OAuthProvider.GOOGLE,
                "google-user-id",
                "user@test.com",
                false,
                "google-user",
                null
        );

        given(socialAccountRepository.findByProviderAndProviderUserId(
                OAuthProvider.GOOGLE,
                "google-user-id"
        )).willReturn(Optional.empty());

        assertThatThrownBy(() -> oauthAuthenticationService.signin(userInfo))
                .isInstanceOf(InvalidCredentialException.class);

        verify(socialAccountRepository, never()).save(any(SocialAccount.class));
        verifyNoInteractions(userRepository, passwordEncoder, authService);
    }

    @Test
    @DisplayName("OAuth sign in fails when email cannot be resolved")
    void signinUnresolvedEmail() {
        OAuthUserInfo userInfo = new OAuthUserInfo(
                OAuthProvider.GOOGLE,
                "google-user-id",
                null,
                false,
                "google-user",
                null
        );

        given(socialAccountRepository.findByProviderAndProviderUserId(
                OAuthProvider.GOOGLE,
                "google-user-id"
        )).willReturn(Optional.empty());

        assertThatThrownBy(() -> oauthAuthenticationService.signin(userInfo))
                .isInstanceOf(InvalidCredentialException.class);

        verifyNoInteractions(userRepository, passwordEncoder, authService);
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
