package com.example.sb10_MoPl_team3.user.controller;

import com.example.sb10_MoPl_team3.global.cursor.CursorResponse;
import com.example.sb10_MoPl_team3.global.security.AuthUser;
import com.example.sb10_MoPl_team3.global.openapi.ApiErrorResponses;
import com.example.sb10_MoPl_team3.user.dto.request.UserLockUpdateRequest;
import com.example.sb10_MoPl_team3.user.dto.request.UserRoleUpdateRequest;
import com.example.sb10_MoPl_team3.user.dto.request.UserSearchCondition;
import com.example.sb10_MoPl_team3.user.dto.response.UserDto;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.service.AdminUserService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Tag(name = "사용자 관리", description = "사용자 및 관리자용 계정 관리 API")
@SecurityRequirement(name = "BearerAuth")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @Operation(summary = "[어드민] 사용자 목록 조회 (커서 페이지네이션)")
    @ApiErrorResponses.Forbidden
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CursorResponse<UserDto>> findUsers(
            @RequestParam(name = "emailLike", required = false) String keyword,
            @RequestParam(required = false) UserRole roleEqual,
            @RequestParam(required = false) Boolean isLocked,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) UUID idAfter,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) String sortBy
    ) {
        UserSearchCondition condition = new UserSearchCondition(
                keyword,
                roleEqual,
                isLocked,
                cursor,
                idAfter,
                limit,
                sortDirection,
                sortBy
        );

        return ResponseEntity.ok(adminUserService.findUsers(condition));
    }

    @PatchMapping("/{userId}/role")
    @Operation(summary = "[어드민] 권한 수정")
    @ApiErrorResponses.Forbidden
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> updateUserRole(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable UUID userId,
            @Valid @RequestBody UserRoleUpdateRequest request
    ) {
        return ResponseEntity.ok(
                adminUserService.updateUserRole(authUser.userId(), userId, request)
        );
    }

    @PatchMapping("/{userId}/locked")
    @Operation(summary = "[어드민] 계정 잠금 상태 변경")
    @ApiErrorResponses.Forbidden
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> updateUserLocked(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable UUID userId,
            @Valid @RequestBody UserLockUpdateRequest request
    ) {
        return ResponseEntity.ok(
                adminUserService.updateUserLocked(authUser.userId(), userId, request)
        );
    }
}
