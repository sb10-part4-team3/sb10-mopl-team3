package com.example.sb10_MoPl_team3.user.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "admin.account")
public record AdminAccountProperties(
        @Email
        @NotBlank
        String email,

        @NotBlank
        String password,

        @NotBlank
        String name
) {
}