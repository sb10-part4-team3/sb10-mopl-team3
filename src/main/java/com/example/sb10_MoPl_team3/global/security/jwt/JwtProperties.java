package com.example.sb10_MoPl_team3.global.security.jwt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.Objects;

@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        @NotBlank
        String secret,

        @NotNull
        Duration accessTokenExpiration,

        @NotNull
        Duration refreshTokenExpiration,

        @NotBlank
        String issuer
) {

    public JwtProperties {
        Objects.requireNonNull(accessTokenExpiration, "accessTokenExpiration must not be null");
        Objects.requireNonNull(refreshTokenExpiration, "refreshTokenExpiration must not be null");

        if (accessTokenExpiration.isZero() || accessTokenExpiration.isNegative()) {
            throw new IllegalArgumentException("accessTokenExpiration must be positive");
        }

        if (refreshTokenExpiration.isZero() || refreshTokenExpiration.isNegative()) {
            throw new IllegalArgumentException("refreshTokenExpiration must be positive");
        }
    }
}
