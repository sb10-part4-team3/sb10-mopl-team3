package com.example.sb10_MoPl_team3.auth.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CsrfController {

    @GetMapping("/api/auth/csrf-token")
    public ResponseEntity<Void> getCsrfToken(CsrfToken csrfToken, HttpServletResponse response) {
        Cookie cookie = new Cookie("XSRF-TOKEN", csrfToken.getToken());
        cookie.setHttpOnly(false);
        cookie.setPath("/");

        response.addCookie(cookie);

        return ResponseEntity.noContent().build();
    }
}
