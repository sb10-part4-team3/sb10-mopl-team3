package com.example.sb10_MoPl_team3.notification.controller;

import com.example.sb10_MoPl_team3.global.security.AuthUser;
import com.example.sb10_MoPl_team3.notification.dto.CursorResponseNotificationDto;
import com.example.sb10_MoPl_team3.notification.dto.NotificationDto;
import com.example.sb10_MoPl_team3.notification.dto.NotificationFindAllRequest;
import com.example.sb10_MoPl_team3.notification.service.NotificationService;
import java.util.UUID;
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
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
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
    public ResponseEntity<Void> read(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable UUID notificationId
    ) {
        notificationService.read(authUser.userId(), notificationId);
        return ResponseEntity.noContent().build();
    }
}
