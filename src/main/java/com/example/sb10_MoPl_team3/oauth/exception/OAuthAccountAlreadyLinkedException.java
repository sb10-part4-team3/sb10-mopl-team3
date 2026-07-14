package com.example.sb10_MoPl_team3.oauth.exception;

public class OAuthAccountAlreadyLinkedException extends RuntimeException {

    public OAuthAccountAlreadyLinkedException() {
        super("OAuth account is already linked");
    }
}