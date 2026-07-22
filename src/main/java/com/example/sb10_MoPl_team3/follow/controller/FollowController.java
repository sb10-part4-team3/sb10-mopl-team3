package com.example.sb10_MoPl_team3.follow.controller;

import com.example.sb10_MoPl_team3.follow.dto.FollowDto;
import com.example.sb10_MoPl_team3.follow.dto.FollowRequest;
import com.example.sb10_MoPl_team3.follow.service.FollowCreateResult;
import com.example.sb10_MoPl_team3.follow.service.FollowService;
import com.example.sb10_MoPl_team3.global.openapi.ApiErrorResponses;
import com.example.sb10_MoPl_team3.global.security.AuthUser;
import jakarta.validation.Valid;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/follows")
@Tag(name = "팔로우 관리", description = "사용자 팔로우 API")
@SecurityRequirement(name = "BearerAuth")
public class FollowController {

    private final FollowService followService;

    @PostMapping
    @Operation(summary = "팔로우")
    @ApiErrorResponses.Common
    @ApiResponse(responseCode = "201", description = "팔로우 생성 성공", content = @Content(schema = @Schema(implementation = FollowDto.class)))
    public ResponseEntity<FollowDto> createFollow(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody FollowRequest request
    ) {
        FollowCreateResult result = followService.create(authUser.userId(), request);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.follow());
    }

    @GetMapping("/count")
    @Operation(summary = "특정 유저의 팔로워 수 조회")
    @ApiErrorResponses.Common
    public ResponseEntity<Long> getFollowerCount(@RequestParam UUID followeeId) {
        return ResponseEntity.ok(followService.getFollowerCount(followeeId));
    }

    @GetMapping("/followed-by-me")
    @Operation(summary = "특정 유저를 내가 팔로우하는지 여부 조회")
    @ApiErrorResponses.NotFound
    public ResponseEntity<FollowDto> isFollowedByMe(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam UUID followeeId
    ) {
        return ResponseEntity.ok(followService.isFollowedByMe(authUser.userId(), followeeId));
    }

    @DeleteMapping("/{followId}")
    @Operation(summary = "팔로우 취소")
    @ApiErrorResponses.Forbidden
    @ApiResponse(responseCode = "204", description = "팔로우 취소 성공")
    public ResponseEntity<Void> cancelFollow(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable UUID followId
    ) {
        followService.cancel(authUser.userId(), followId);
        return ResponseEntity.noContent().build();
    }
}
