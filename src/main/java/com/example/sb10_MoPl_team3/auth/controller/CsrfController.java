package com.example.sb10_MoPl_team3.auth.controller;

import com.example.sb10_MoPl_team3.global.openapi.ApiErrorResponses;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "인증 관리", description = "인증 및 CSRF 토큰 API")
public class CsrfController {

    @GetMapping("/api/auth/csrf-token")
    @Operation(summary = "CSRF 토큰 조회", description = "CSRF 토큰을 XSRF-TOKEN 쿠키로 발급합니다.")
    @ApiErrorResponses.Public
    @ApiResponse(responseCode = "204", description = "CSRF 토큰 발급 성공")
    public ResponseEntity<Void> getCsrfToken(CsrfToken csrfToken, HttpServletResponse response) {
        Cookie cookie = new Cookie("XSRF-TOKEN", csrfToken.getToken());
        cookie.setHttpOnly(false);
        cookie.setPath("/");

        response.addCookie(cookie);

        return ResponseEntity.noContent().build();
    }
}
