package com.example.sb10_MoPl_team3.oauth.exception;

public class OAuthAccountNotLinkedException extends RuntimeException {

    public OAuthAccountNotLinkedException() {
        super("OAuth account is not linked");
    }
}