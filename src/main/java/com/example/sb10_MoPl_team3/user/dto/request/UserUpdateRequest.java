package com.example.sb10_MoPl_team3.user.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Size(max = 100, message = "이름은 100자 이하여야 합니다.")
        @Pattern(regexp = ".*\\S.*", message = "이름은 공백만 입력할 수 없습니다.")
        String name
) {
}