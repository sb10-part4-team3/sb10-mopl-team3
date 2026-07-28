package com.example.sb10_MoPl_team3.notification.controller;

import com.example.sb10_MoPl_team3.global.security.AuthUser;
import com.example.sb10_MoPl_team3.global.openapi.ApiErrorResponses;
import com.example.sb10_MoPl_team3.notification.dto.CursorResponseNotificationDto;
import com.example.sb10_MoPl_team3.notification.dto.NotificationDto;
import com.example.sb10_MoPl_team3.notification.dto.NotificationFindAllRequest;
import com.example.sb10_MoPl_team3.notification.service.NotificationService;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
@Tag(name = "알림", description = "알림 API")
@SecurityRequirement(name = "BearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "알림 목록 조회 (커서 페이지네이션)")
    @ApiErrorResponses.Common
    public ResponseEntity<CursorResponseNotificationDto<NotificationDto>> findNotifications(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) UUID idAfter,
            @RequestParam int limit,
            @RequestParam String sortDirection,
            @RequestParam String sortBy
    ) {
        NotificationFindAllRequest request = new NotificationFindAllRequest(
                cursor,
                idAfter,
                limit,
                sortDirection,
                sortBy
        );
        CursorResponseNotificationDto<NotificationDto> response =
                notificationService.findAll(authUser.userId(), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{notificationId}")
    @Operation(summary = "알림 읽음 처리")
    @ApiErrorResponses.Common
    @ApiResponse(responseCode = "204", description = "읽음 처리 성공")
    public ResponseEntity<Void> read(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable UUID notificationId
    ) {
        notificationService.read(authUser.userId(), notificationId);
        return ResponseEntity.noContent().build();
    }
}
