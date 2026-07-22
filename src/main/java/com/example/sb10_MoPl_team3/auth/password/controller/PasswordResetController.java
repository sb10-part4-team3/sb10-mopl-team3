package com.example.sb10_MoPl_team3.auth.password.controller;

import com.example.sb10_MoPl_team3.auth.password.dto.TemporaryPasswordIssueRequest;
import com.example.sb10_MoPl_team3.auth.password.service.PasswordResetService;
import com.example.sb10_MoPl_team3.global.openapi.ApiErrorResponses;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/reset-password")
@RequiredArgsConstructor
@Tag(name = "인증 관리", description = "인증 및 비밀번호 초기화 API")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping
    @Operation(summary = "비밀번호 초기화", description = "가입한 이메일로 임시 비밀번호를 발급합니다.")
    @ApiErrorResponses.PublicNotFound
    @ApiResponse(responseCode = "204", description = "임시 비밀번호 발급 성공")
    public ResponseEntity<Void> issueTemporaryPassword(
            @Valid @RequestBody TemporaryPasswordIssueRequest request
    ) {
        passwordResetService.issueTemporaryPassword(request);
        return ResponseEntity.noContent().build();
    }
}
