package com.example.sb10_MoPl_team3.domain.auth.dto.response;

import com.example.sb10_MoPl_team3.domain.user.dto.response.UserDto;

public record JwtDto(
        UserDto userDto,
        String accessToken
) {
}