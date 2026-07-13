package com.example.sb10_MoPl_team3.oauth.service;

import com.example.sb10_MoPl_team3.oauth.dto.OAuthUserInfo;
import com.example.sb10_MoPl_team3.oauth.dto.OAuthUserPrincipal;
import com.example.sb10_MoPl_team3.oauth.enums.OAuthProvider;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final OAuthUserInfoConverter converter;
    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;

    public CustomOAuth2UserService(OAuthUserInfoConverter converter) {
        this(converter, new DefaultOAuth2UserService());
    }

    CustomOAuth2UserService(
            OAuthUserInfoConverter converter,
            OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate
    ) {
        this.converter = converter;
        this.delegate = delegate;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = delegate.loadUser(userRequest);
        OAuthProvider provider = resolveProvider(userRequest);

        try {
            OAuthUserInfo userInfo = converter.convert(provider, oauth2User.getAttributes());

            return new OAuthUserPrincipal(
                    userInfo,
                    oauth2User.getAttributes(),
                    oauth2User.getAuthorities()
            );
        } catch (RuntimeException exception) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_oauth_user_info"),
                    "OAuth user info is invalid",
                    exception
            );
        }
    }

    private OAuthProvider resolveProvider(OAuth2UserRequest userRequest) {
        String registrationId = userRequest
                .getClientRegistration()
                .getRegistrationId();

        try {
            return OAuthProvider.fromRegistrationId(registrationId);
        } catch (IllegalArgumentException exception) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("unsupported_oauth_provider"),
                    exception.getMessage(),
                    exception
            );
        }
    }
}