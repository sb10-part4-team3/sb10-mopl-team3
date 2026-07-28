package com.example.sb10_MoPl_team3.oauth.service;

import com.example.sb10_MoPl_team3.oauth.dto.OAuthUserInfo;
import com.example.sb10_MoPl_team3.oauth.enums.OAuthProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthUserInfoConverterTest {

    private final OAuthUserInfoConverter converter = new OAuthUserInfoConverter();

    @Test
    @DisplayName("Google 사용자 정보를 내부 OAuthUserInfo로 변환한다")
    void convertGoogle() {
        Map<String, Object> attributes = Map.of(
                "sub", "google-user-id",
                "email", "google@test.com",
                "email_verified", true,
                "name", "Google User",
                "picture", "https://example.com/google-profile.png"
        );

        OAuthUserInfo userInfo = converter.convert(OAuthProvider.GOOGLE, attributes);

        assertThat(userInfo.provider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(userInfo.providerUserId()).isEqualTo("google-user-id");
        assertThat(userInfo.email()).isEqualTo("google@test.com");
        assertThat(userInfo.emailVerified()).isTrue();
        assertThat(userInfo.name()).isEqualTo("Google User");
        assertThat(userInfo.profileImageUrl()).isEqualTo("https://example.com/google-profile.png");
    }

    @Test
    @DisplayName("Kakao 사용자 정보를 내부 OAuthUserInfo로 변환한다")
    void convertKakao() {
        Map<String, Object> attributes = Map.of(
                "id", 12345L,
                "properties", Map.of(
                        "nickname", "Kakao User",
                        "profile_image", "https://example.com/kakao-profile.png"
                ),
                "kakao_account", Map.of(
                        "email", "kakao@test.com",
                        "is_email_verified", true
                )
        );

        OAuthUserInfo userInfo = converter.convert(OAuthProvider.KAKAO, attributes);

        assertThat(userInfo.provider()).isEqualTo(OAuthProvider.KAKAO);
        assertThat(userInfo.providerUserId()).isEqualTo("12345");
        assertThat(userInfo.email()).isEqualTo("kakao@test.com");
        assertThat(userInfo.emailVerified()).isTrue();
        assertThat(userInfo.name()).isEqualTo("Kakao User");
        assertThat(userInfo.profileImageUrl()).isEqualTo("https://example.com/kakao-profile.png");
    }

    @Test
    @DisplayName("Kakao 이메일이 없으면 가상 이메일을 사용할 수 있다")
    void convertKakaoWithoutEmail() {
        Map<String, Object> attributes = Map.of(
                "id", 12345L,
                "properties", Map.of(
                        "nickname", "Kakao User"
                )
        );

        OAuthUserInfo userInfo = converter.convert(OAuthProvider.KAKAO, attributes);

        assertThat(userInfo.email()).isNull();
        assertThat(userInfo.resolvedEmail()).isEqualTo("Kakao User_12345@kakao.com");
    }

    @Test
    @DisplayName("OAuth response map can contain null values")
    void convertKakaoWithNullValues() {
        Map<String, Object> kakaoAccount = new HashMap<>();
        kakaoAccount.put("email", null);
        kakaoAccount.put("profile", null);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 12345L);
        attributes.put("kakao_account", kakaoAccount);

        OAuthUserInfo userInfo = converter.convert(OAuthProvider.KAKAO, attributes);

        assertThat(userInfo.providerUserId()).isEqualTo("12345");
        assertThat(userInfo.email()).isNull();
        assertThat(userInfo.emailVerified()).isFalse();
        assertThat(userInfo.name()).isEqualTo("12345");
    }
}
