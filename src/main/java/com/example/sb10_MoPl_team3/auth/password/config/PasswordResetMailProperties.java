package com.example.sb10_MoPl_team3.auth.password.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "auth.password-reset-mail")
public record PasswordResetMailProperties(
        @Email
        @NotBlank
        String from
) {
}