package com.example.sb10_MoPl_team3.auth.password.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record TemporaryPasswordIssueRequest(
        @Email
        @NotBlank
        String email
) {
}
