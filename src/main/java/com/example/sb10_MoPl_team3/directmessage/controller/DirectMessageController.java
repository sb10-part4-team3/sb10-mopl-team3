package com.example.sb10_MoPl_team3.directmessage.controller;

import com.example.sb10_MoPl_team3.directmessage.dto.DirectMessageDto;
import com.example.sb10_MoPl_team3.directmessage.dto.response.CursorResponseDirectMessageDto;
import com.example.sb10_MoPl_team3.directmessage.service.DirectMessageService;
import com.example.sb10_MoPl_team3.global.security.AuthUser;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/conversations/{conversationId}/direct-messages")
public class DirectMessageController {

    private final DirectMessageService directMessageService;

    @GetMapping
    public ResponseEntity<CursorResponseDirectMessageDto<DirectMessageDto>> findDirectMessages(
        @AuthenticationPrincipal AuthUser authUser,
        @PathVariable UUID conversationId,
        @RequestParam(required = false) String cursor,
        @RequestParam(required = false) UUID idAfter,
        @RequestParam int limit,
        @RequestParam String sortDirection,
        @RequestParam String sortBy
    ) {
        CursorResponseDirectMessageDto<DirectMessageDto> response = directMessageService.findAll(
            authUser.userId(),
            conversationId,
            cursor,
            idAfter,
            limit,
            sortDirection,
            sortBy
        );
        return ResponseEntity.ok(response);
    }
}
