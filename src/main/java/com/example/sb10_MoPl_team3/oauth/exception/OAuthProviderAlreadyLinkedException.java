package com.example.sb10_MoPl_team3.oauth.exception;

public class OAuthProviderAlreadyLinkedException extends RuntimeException {

    public OAuthProviderAlreadyLinkedException() {
        super("OAuth provider is already linked to user");
    }
}