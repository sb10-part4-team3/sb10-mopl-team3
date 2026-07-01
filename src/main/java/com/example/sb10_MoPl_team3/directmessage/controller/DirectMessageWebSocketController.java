package com.example.sb10_MoPl_team3.directmessage.controller;

import com.example.sb10_MoPl_team3.directmessage.dto.DirectMessageSendRequest;
import com.example.sb10_MoPl_team3.directmessage.service.DirectMessageAsyncService;
import com.example.sb10_MoPl_team3.global.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Controller
@RequiredArgsConstructor
public class DirectMessageWebSocketController {

    private static final String DESTINATION_FORMAT =
            "/sub/conversations/%s/direct-messages";

    private final DirectMessageAsyncService directMessageAsyncService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/conversations/{conversationId}/direct-messages")
    public CompletableFuture<Void> send(
            @DestinationVariable UUID conversationId,
            @Valid @Payload DirectMessageSendRequest request,
            Authentication authentication
    ) {
        AuthUser authUser = extractAuthUser(authentication);
        return directMessageAsyncService.saveAsync(
                        conversationId, authUser.userId(), request.content())
                .thenAccept(message -> messagingTemplate.convertAndSend(
                        DESTINATION_FORMAT.formatted(conversationId), message));
    }

    private AuthUser extractAuthUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser authUser)) {
            throw new MessagingException("인증된 사용자만 쪽지를 전송할 수 있습니다.");
        }
        return authUser;
    }
}
