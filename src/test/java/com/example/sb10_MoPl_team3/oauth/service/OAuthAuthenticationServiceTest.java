package com.example.sb10_MoPl_team3.oauth.service;

import com.example.sb10_MoPl_team3.auth.dto.AuthTokenResult;
import com.example.sb10_MoPl_team3.auth.dto.response.JwtDto;
import com.example.sb10_MoPl_team3.auth.service.AuthService;
import com.example.sb10_MoPl_team3.oauth.dto.OAuthUserInfo;
import com.example.sb10_MoPl_team3.oauth.entity.SocialAccount;
import com.example.sb10_MoPl_team3.oauth.enums.OAuthProvider;
import com.example.sb10_MoPl_team3.oauth.exception.OAuthAccountNotLinkedException;
import com.example.sb10_MoPl_team3.oauth.repository.SocialAccountRepository;
import com.example.sb10_MoPl_team3.user.dto.response.UserDto;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OAuthAuthenticationServiceTest {

    @Mock
    private SocialAccountRepository socialAccountRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private OAuthAuthenticationService oauthAuthenticationService;

    @Test
    @DisplayName("연동된 소셜 계정이면 기존 인증 토큰 발급 흐름으로 로그인한다")
    void signinLinkedAccount() {
        User user = new User(
                "user@test.com",
                "테스트 사용자",
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
                "테스트 사용자",
                null
        );

        AuthTokenResult tokenResult = new AuthTokenResult(
                new JwtDto(
                        new UserDto(
                                UUID.randomUUID(),
                                Instant.now(),
                                "user@test.com",
                                "테스트 사용자",
                                null,
                                UserRole.USER,
                                false
                        ),
                        "access-token"
                ),
                "refresh-token"
        );

        given(socialAccountRepository.findByProviderAndProviderUserId(
                OAuthProvider.GOOGLE,
                "google-user-id"
        )).willReturn(Optional.of(socialAccount));
        given(authService.issueTokenForAuthenticatedUser(user)).willReturn(tokenResult);

        AuthTokenResult result = oauthAuthenticationService.signin(userInfo);

        assertThat(result).isSameAs(tokenResult);

        verify(authService).issueTokenForAuthenticatedUser(user);
    }

    @Test
    @DisplayName("연동되지 않은 소셜 계정이면 로그인에 실패한다")
    void signinUnlinkedAccount() {
        OAuthUserInfo userInfo = new OAuthUserInfo(
                OAuthProvider.KAKAO,
                "kakao-user-id",
                null,
                "카카오 사용자",
                null
        );

        given(socialAccountRepository.findByProviderAndProviderUserId(
                OAuthProvider.KAKAO,
                "kakao-user-id"
        )).willReturn(Optional.empty());

        assertThatThrownBy(() -> oauthAuthenticationService.signin(userInfo))
                .isInstanceOf(OAuthAccountNotLinkedException.class);
    }
}