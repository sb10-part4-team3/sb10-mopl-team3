package com.example.sb10_MoPl_team3.auth.password.controller;

import com.example.sb10_MoPl_team3.auth.password.dto.TemporaryPasswordIssueRequest;
import com.example.sb10_MoPl_team3.auth.password.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/reset-password")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping
    public ResponseEntity<Void> issueTemporaryPassword(
            @Valid @RequestBody TemporaryPasswordIssueRequest request
    ) {
        passwordResetService.issueTemporaryPassword(request);
        return ResponseEntity.noContent().build();
    }
}
