package com.example.sb10_MoPl_team3.user.controller;

import com.example.sb10_MoPl_team3.global.openapi.ApiErrorResponses;
import com.example.sb10_MoPl_team3.user.dto.request.UserCreateRequest;
import com.example.sb10_MoPl_team3.user.dto.response.UserDto;
import com.example.sb10_MoPl_team3.user.service.UserService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.sb10_MoPl_team3.user.dto.request.UserUpdateRequest;
import org.springframework.web.multipart.MultipartFile;
import com.example.sb10_MoPl_team3.user.dto.request.UserPasswordUpdateRequest;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Tag(name = "사용자 관리", description = "사용자 및 프로필 관리 API")
@SecurityRequirement(name = "BearerAuth")
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(summary = "사용자 등록 (회원가입)", security = {})
    @ApiErrorResponses.Common
    @ApiResponse(responseCode = "201", description = "회원가입 성공", content = @Content(schema = @Schema(implementation = UserDto.class)))
    public ResponseEntity<UserDto> createUser(
            @Valid @RequestBody UserCreateRequest request
    ) {
        UserDto response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "사용자 상세 조회")
    @ApiErrorResponses.NotFound
    public ResponseEntity<UserDto> findUser(
            @PathVariable UUID userId
    ) {
        UserDto response = userService.findUser(userId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping(
            value = "/{userId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(summary = "프로필 변경", description = "본인의 프로필만 변경할 수 있습니다.")
    @ApiErrorResponses.Forbidden
    public ResponseEntity<UserDto> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestPart("request") UserUpdateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        UserDto response = userService.updateUser(userId, request, image);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{userId}/password")
    @Operation(summary = "비밀번호 변경", description = "본인의 비밀번호만 변경할 수 있습니다.")
    @ApiErrorResponses.Forbidden
    @ApiResponse(responseCode = "204", description = "비밀번호 변경 성공")
    public ResponseEntity<Void> changePassword(
            @PathVariable UUID userId,
            @Valid @RequestBody UserPasswordUpdateRequest request
    ) {
        userService.changePassword(userId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "회원 탈퇴", description = "본인 계정을 탈퇴 처리합니다.")
    @ApiErrorResponses.Forbidden
    @ApiResponse(responseCode = "204", description = "회원 탈퇴 성공")
    public ResponseEntity<Void> withdrawUser(
            @PathVariable UUID userId
    ) {
        userService.withdrawUser(userId);
        return ResponseEntity.noContent().build();
    }
}
