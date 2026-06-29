package com.example.sb10_MoPl_team3.auth.dto.response;

import com.example.sb10_MoPl_team3.user.dto.response.UserDto;

public record JwtDto(
        UserDto userDto,
        String accessToken
) {
}
