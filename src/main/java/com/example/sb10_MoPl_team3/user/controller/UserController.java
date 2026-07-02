package com.example.sb10_MoPl_team3.user.controller;

import com.example.sb10_MoPl_team3.user.dto.request.UserCreateRequest;
import com.example.sb10_MoPl_team3.user.dto.response.UserDto;
import com.example.sb10_MoPl_team3.user.service.UserService;
import jakarta.validation.Valid;
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
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserDto> createUser(
            @Valid @RequestBody UserCreateRequest request
    ) {
        UserDto response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{userId}")
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
    public ResponseEntity<UserDto> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestPart("request") UserUpdateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        UserDto response = userService.updateUser(userId, request, image);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{userId}/password")
    public ResponseEntity<Void> changePassword(
            @PathVariable UUID userId,
            @Valid @RequestBody UserPasswordUpdateRequest request
    ) {
        userService.changePassword(userId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> withdrawUser(
            @PathVariable UUID userId
    ) {
        userService.withdrawUser(userId);
        return ResponseEntity.noContent().build();
    }
}
