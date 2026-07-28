package com.example.sb10_MoPl_team3.oauth.service;

import com.example.sb10_MoPl_team3.auth.exception.InvalidCredentialException;
import com.example.sb10_MoPl_team3.oauth.dto.OAuthUserInfo;
import com.example.sb10_MoPl_team3.oauth.entity.SocialAccount;
import com.example.sb10_MoPl_team3.oauth.enums.OAuthProvider;
import com.example.sb10_MoPl_team3.oauth.repository.SocialAccountRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OAuthSocialAccountCreatorTest {

    @Mock
    private SocialAccountRepository socialAccountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private OAuthSocialAccountCreator socialAccountCreator;

    @Test
    @DisplayName("Unlinked OAuth account creates user and social account")
    void createWithNewUser() {
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

        given(userRepository.findByEmail("kakao-user_kakao-user-id@kakao.com"))
                .willReturn(Optional.empty());
        given(passwordEncoder.encode(any(String.class))).willReturn("encoded-random-password");
        given(userRepository.saveAndFlush(any(User.class))).willReturn(savedUser);
        given(socialAccountRepository.saveAndFlush(any(SocialAccount.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        SocialAccount result = socialAccountCreator.create(userInfo);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());

        User createdUser = userCaptor.getValue();
        assertThat(createdUser.getEmail()).isEqualTo("kakao-user_kakao-user-id@kakao.com");
        assertThat(createdUser.getName()).isEqualTo("kakao-user");
        assertThat(createdUser.getProfileImageUrl()).isEqualTo("https://image.test/profile.png");
        assertThat(createdUser.getRole()).isEqualTo(UserRole.USER);

        assertThat(result.getUser()).isSameAs(savedUser);
        assertThat(result.getProvider()).isEqualTo(OAuthProvider.KAKAO);
        assertThat(result.getProviderUserId()).isEqualTo("kakao-user-id");
        assertThat(result.getProviderEmail()).isNull();
    }

    @Test
    @DisplayName("Verified OAuth email can use existing user")
    void createWithExistingEmailUser() {
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

        given(userRepository.findByEmail("user@test.com")).willReturn(Optional.of(existingUser));
        given(socialAccountRepository.saveAndFlush(any(SocialAccount.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        SocialAccount result = socialAccountCreator.create(userInfo);

        assertThat(result.getUser()).isSameAs(existingUser);
        assertThat(result.getProvider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(result.getProviderUserId()).isEqualTo("google-user-id");
        assertThat(result.getProviderEmail()).isEqualTo("user@test.com");

        verify(userRepository).findByEmail("user@test.com");
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("Unverified provider email cannot be used")
    void createWithUnverifiedProviderEmail() {
        OAuthUserInfo userInfo = new OAuthUserInfo(
                OAuthProvider.GOOGLE,
                "google-user-id",
                "user@test.com",
                false,
                "google-user",
                null
        );

        assertThatThrownBy(() -> socialAccountCreator.create(userInfo))
                .isInstanceOf(InvalidCredentialException.class);

        verify(socialAccountRepository, never()).saveAndFlush(any(SocialAccount.class));
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("OAuth account creation fails when email cannot be resolved")
    void createWithUnresolvedEmail() {
        OAuthUserInfo userInfo = new OAuthUserInfo(
                OAuthProvider.GOOGLE,
                "google-user-id",
                null,
                false,
                "google-user",
                null
        );

        assertThatThrownBy(() -> socialAccountCreator.create(userInfo))
                .isInstanceOf(InvalidCredentialException.class);

        verifyNoInteractions(userRepository, passwordEncoder, socialAccountRepository);
    }
}
