package com.example.sb10_MoPl_team3.follow.controller;

import com.example.sb10_MoPl_team3.follow.dto.FollowDto;
import com.example.sb10_MoPl_team3.follow.dto.FollowRequest;
import com.example.sb10_MoPl_team3.follow.service.FollowService;
import com.example.sb10_MoPl_team3.global.security.AuthUser;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/follows")
public class FollowController {

    private final FollowService followService;

    @PostMapping
    public ResponseEntity<FollowDto> createFollow(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody FollowRequest request
    ) {
        FollowDto response = followService.create(authUser.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{followId}")
    public ResponseEntity<Void> cancelFollow(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable UUID followId
    ) {
        followService.cancel(authUser.userId(), followId);
        return ResponseEntity.noContent().build();
    }
}
