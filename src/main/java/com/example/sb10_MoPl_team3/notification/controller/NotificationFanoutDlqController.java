package com.example.sb10_MoPl_team3.notification.controller;

import com.example.sb10_MoPl_team3.notification.dto.NotificationFanoutDlqDto;
import com.example.sb10_MoPl_team3.global.openapi.ApiErrorResponses;
import com.example.sb10_MoPl_team3.notification.service.NotificationFanoutDlqService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/admin/notifications/fanout-dlq")
@Tag(name = "알림 DLQ 관리", description = "관리자용 알림 팬아웃 DLQ API")
@SecurityRequirement(name = "BearerAuth")
public class NotificationFanoutDlqController {

    private final NotificationFanoutDlqService dlqService;

    @GetMapping
    @Operation(summary = "[어드민] 재처리 대기 DLQ 목록 조회")
    @ApiErrorResponses.Forbidden
    @PreAuthorize("hasRole('ADMIN')")
    public List<NotificationFanoutDlqDto> findPending(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        return dlqService.findPending(limit);
    }

    @PostMapping("/{dlqId}/retry")
    @Operation(summary = "[어드민] DLQ 알림 재처리")
    @ApiErrorResponses.Forbidden
    @PreAuthorize("hasRole('ADMIN')")
    public NotificationFanoutDlqDto retry(@PathVariable UUID dlqId) {
        return dlqService.retry(dlqId);
    }
}
