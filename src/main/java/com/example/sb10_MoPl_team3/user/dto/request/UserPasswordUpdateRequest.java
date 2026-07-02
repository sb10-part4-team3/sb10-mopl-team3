package com.example.sb10_MoPl_team3.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UserPasswordUpdateRequest(
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {
}