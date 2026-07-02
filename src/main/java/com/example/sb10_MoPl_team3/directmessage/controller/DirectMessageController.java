package com.example.sb10_MoPl_team3.directmessage.controller;

import com.example.sb10_MoPl_team3.directmessage.dto.DirectMessageDto;
import com.example.sb10_MoPl_team3.directmessage.dto.DirectMessageReadStatusChange;
import com.example.sb10_MoPl_team3.directmessage.dto.response.CursorResponseDirectMessageDto;
import com.example.sb10_MoPl_team3.directmessage.service.DirectMessageService;
import com.example.sb10_MoPl_team3.global.security.AuthUser;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/conversations/{conversationId}/direct-messages")
public class DirectMessageController {

    private final DirectMessageService directMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/{directMessageId}/read")
    public ResponseEntity<Void> read(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable UUID conversationId,
            @PathVariable UUID directMessageId
    ) {
        DirectMessageReadStatusChange change = directMessageService.read(
                authUser.userId(), conversationId, directMessageId);
        try {
            messagingTemplate.convertAndSend(
                    "/sub/conversations/%s/direct-messages".formatted(conversationId),
                    change
            );
        } catch (MessagingException e) {
            log.warn(
                    "DM 읽음 상태 브로드캐스트 실패: conversationId={}, directMessageId={}",
                    conversationId,
                    directMessageId,
                    e
            );
        }
        return ResponseEntity.ok().build();
    }

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
