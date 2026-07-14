package com.example.sb10_MoPl_team3.oauth.service;

import com.example.sb10_MoPl_team3.global.security.exception.AccessDeniedBusinessException;
import com.example.sb10_MoPl_team3.oauth.dto.OAuthUserInfo;
import com.example.sb10_MoPl_team3.oauth.entity.SocialAccount;
import com.example.sb10_MoPl_team3.oauth.enums.OAuthProvider;
import com.example.sb10_MoPl_team3.oauth.exception.OAuthAccountAlreadyLinkedException;
import com.example.sb10_MoPl_team3.oauth.exception.OAuthProviderAlreadyLinkedException;
import com.example.sb10_MoPl_team3.oauth.repository.SocialAccountRepository;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.enums.UserStatus;
import com.example.sb10_MoPl_team3.user.exception.UserNotFoundException;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OAuthLinkServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SocialAccountRepository socialAccountRepository;

    @InjectMocks
    private OAuthLinkService oauthLinkService;

    @Test
    @DisplayName("사용자 계정에 OAuth 계정을 연동한다")
    void link() {
        UUID userId = UUID.randomUUID();
        User user = user();
        OAuthUserInfo userInfo = googleUserInfo();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(socialAccountRepository.existsByProviderAndProviderUserId(
                OAuthProvider.GOOGLE,
                "google-user-id"
        )).willReturn(false);
        given(socialAccountRepository.existsByUser_IdAndProvider(
                userId,
                OAuthProvider.GOOGLE
        )).willReturn(false);
        given(socialAccountRepository.save(any(SocialAccount.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        SocialAccount result = oauthLinkService.link(userId, userInfo);

        ArgumentCaptor<SocialAccount> captor = ArgumentCaptor.forClass(SocialAccount.class);
        verify(socialAccountRepository).save(captor.capture());

        SocialAccount saved = captor.getValue();

        assertThat(result).isSameAs(saved);
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getProvider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(saved.getProviderUserId()).isEqualTo("google-user-id");
        assertThat(saved.getProviderEmail()).isEqualTo("google@test.com");
    }

    @Test
    @DisplayName("사용자를 찾을 수 없으면 OAuth 계정 연동에 실패한다")
    void linkUserNotFound() {
        UUID userId = UUID.randomUUID();

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> oauthLinkService.link(userId, googleUserInfo()))
                .isInstanceOf(UserNotFoundException.class);

        verify(socialAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("잠금 계정은 OAuth 계정을 연동할 수 없다")
    void linkLockedUser() {
        UUID userId = UUID.randomUUID();
        User user = user();
        user.changeStatus(UserStatus.LOCKED);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> oauthLinkService.link(userId, googleUserInfo()))
                .isInstanceOf(AccessDeniedBusinessException.class);

        verify(socialAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 다른 사용자에게 연동된 OAuth 계정이면 연동에 실패한다")
    void linkAlreadyLinkedOAuthAccount() {
        UUID userId = UUID.randomUUID();

        given(userRepository.findById(userId)).willReturn(Optional.of(user()));
        given(socialAccountRepository.existsByProviderAndProviderUserId(
                OAuthProvider.GOOGLE,
                "google-user-id"
        )).willReturn(true);

        assertThatThrownBy(() -> oauthLinkService.link(userId, googleUserInfo()))
                .isInstanceOf(OAuthAccountAlreadyLinkedException.class);

        verify(socialAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("사용자에게 같은 provider가 이미 연동되어 있으면 연동에 실패한다")
    void linkProviderAlreadyLinkedToUser() {
        UUID userId = UUID.randomUUID();

        given(userRepository.findById(userId)).willReturn(Optional.of(user()));
        given(socialAccountRepository.existsByProviderAndProviderUserId(
                OAuthProvider.GOOGLE,
                "google-user-id"
        )).willReturn(false);
        given(socialAccountRepository.existsByUser_IdAndProvider(
                userId,
                OAuthProvider.GOOGLE
        )).willReturn(true);

        assertThatThrownBy(() -> oauthLinkService.link(userId, googleUserInfo()))
                .isInstanceOf(OAuthProviderAlreadyLinkedException.class);

        verify(socialAccountRepository, never()).save(any());
    }

    private User user() {
        return new User(
                "user@test.com",
                "사용자",
                "encoded-password",
                null,
                UserRole.USER
        );
    }

    private OAuthUserInfo googleUserInfo() {
        return new OAuthUserInfo(
                OAuthProvider.GOOGLE,
                "google-user-id",
                "google@test.com",
                "Google User",
                "https://example.com/profile.png"
        );
    }
}