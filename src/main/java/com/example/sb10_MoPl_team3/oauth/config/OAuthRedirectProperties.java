package com.example.sb10_MoPl_team3.oauth.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "oauth.redirect")
public record OAuthRedirectProperties(
        @NotBlank
        String successUri,

        @NotBlank
        String failureUri
) {
}