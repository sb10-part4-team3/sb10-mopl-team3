package com.example.sb10_MoPl_team3.auth.dto;

import com.example.sb10_MoPl_team3.auth.dto.response.JwtDto;

public record AuthTokenResult(
        JwtDto jwtDto,
        String refreshToken
) {
    @Override
    public String toString() {
        return "AuthTokenResult[jwtDto=" + jwtDto + ", refreshToken=PROTECTED]";
    }
}