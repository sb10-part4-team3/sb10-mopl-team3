package com.example.sb10_MoPl_team3.oauth.service;

import com.example.sb10_MoPl_team3.oauth.dto.OAuthUserPrincipal;
import com.example.sb10_MoPl_team3.oauth.enums.OAuthProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomOAuth2UserServiceTest {

    private final OAuthUserInfoConverter converter = new OAuthUserInfoConverter();

    @Test
    @DisplayName("Google OAuth2User를 OAuthUserPrincipal로 변환한다")
    void loadGoogleUser() {
        OAuth2User delegateUser = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of(
                        "sub", "google-user-id",
                        "email", "google@test.com",
                        "name", "Google User",
                        "picture", "https://example.com/google.png"
                ),
                "sub"
        );

        CustomOAuth2UserService service = new CustomOAuth2UserService(
                converter,
                request -> delegateUser
        );

        OAuth2User result = service.loadUser(userRequest("google", "sub"));

        assertThat(result).isInstanceOf(OAuthUserPrincipal.class);

        OAuthUserPrincipal principal = (OAuthUserPrincipal) result;

        assertThat(principal.userInfo().provider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(principal.userInfo().providerUserId()).isEqualTo("google-user-id");
        assertThat(principal.userInfo().email()).isEqualTo("google@test.com");
        assertThat(principal.userInfo().name()).isEqualTo("Google User");
    }

    @Test
    @DisplayName("Kakao OAuth2User를 OAuthUserPrincipal로 변환한다")
    void loadKakaoUser() {
        OAuth2User delegateUser = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of(
                        "id", 12345L,
                        "properties", Map.of(
                                "nickname", "Kakao User",
                                "profile_image", "https://example.com/kakao.png"
                        ),
                        "kakao_account", Map.of(
                                "email", "kakao@test.com"
                        )
                ),
                "id"
        );

        CustomOAuth2UserService service = new CustomOAuth2UserService(
                converter,
                request -> delegateUser
        );

        OAuth2User result = service.loadUser(userRequest("kakao", "id"));

        assertThat(result).isInstanceOf(OAuthUserPrincipal.class);

        OAuthUserPrincipal principal = (OAuthUserPrincipal) result;

        assertThat(principal.userInfo().provider()).isEqualTo(OAuthProvider.KAKAO);
        assertThat(principal.userInfo().providerUserId()).isEqualTo("12345");
        assertThat(principal.userInfo().email()).isEqualTo("kakao@test.com");
        assertThat(principal.userInfo().name()).isEqualTo("Kakao User");
    }

    @Test
    @DisplayName("지원하지 않는 OAuth provider면 예외가 발생한다")
    void loadUnsupportedProvider() {
        OAuth2User delegateUser = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("id", "test-id"),
                "id"
        );

        CustomOAuth2UserService service = new CustomOAuth2UserService(
                converter,
                request -> delegateUser
        );

        assertThatThrownBy(() -> service.loadUser(userRequest("naver", "id")))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    private OAuth2UserRequest userRequest(String registrationId, String userNameAttributeName) {
        ClientRegistration clientRegistration = ClientRegistration
                .withRegistrationId(registrationId)
                .clientId("client-id")
                .clientSecret("client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://example.com/oauth/authorize")
                .tokenUri("https://example.com/oauth/token")
                .userInfoUri("https://example.com/oauth/userinfo")
                .userNameAttributeName(userNameAttributeName)
                .clientName(registrationId)
                .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "access-token",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );

        return new OAuth2UserRequest(clientRegistration, accessToken);
    }
}