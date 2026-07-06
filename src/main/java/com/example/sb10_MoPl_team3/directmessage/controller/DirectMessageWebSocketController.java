package com.example.sb10_MoPl_team3.directmessage.controller;

import com.example.sb10_MoPl_team3.directmessage.dto.DirectMessageSendRequest;
import com.example.sb10_MoPl_team3.directmessage.dto.DirectMessageDto;
import com.example.sb10_MoPl_team3.directmessage.service.DirectMessageAsyncService;
import com.example.sb10_MoPl_team3.directmessage.service.DirectMessageConversationPresence;
import com.example.sb10_MoPl_team3.global.security.AuthUser;
import com.example.sb10_MoPl_team3.global.sse.SseEventPublisher;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.core.task.TaskRejectedException;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Controller
@RequiredArgsConstructor
public class DirectMessageWebSocketController {

    private static final String DESTINATION_FORMAT =
            "/sub/conversations/%s/direct-messages";

    private final DirectMessageAsyncService directMessageAsyncService;
    private final SimpMessagingTemplate messagingTemplate;
    private final DirectMessageConversationPresence conversationPresence;
    private final SseEventPublisher sseEventPublisher;

    @MessageMapping("/conversations/{conversationId}/direct-messages")
    public CompletableFuture<Void> send(
            @DestinationVariable UUID conversationId,
            @Valid @Payload DirectMessageSendRequest request,
            Authentication authentication
    ) {
        AuthUser authUser = extractAuthUser(authentication);
        try {
            return directMessageAsyncService.saveAsync(
                            conversationId, authUser.userId(), request.content())
                    .thenAccept(message -> publish(conversationId, message));
        } catch (TaskRejectedException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, exception);
        }
    }

    private void publish(UUID conversationId, DirectMessageDto message) {
        messagingTemplate.convertAndSend(
                DESTINATION_FORMAT.formatted(conversationId), message);
        UUID receiverId = message.receiver().userId();
        if (!conversationPresence.isActive(receiverId, conversationId)) {
            sseEventPublisher.publish(
                    receiverId,
                    SseEventPublisher.DIRECT_MESSAGES_EVENT,
                    message);
        }
    }

    private AuthUser extractAuthUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser authUser)) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIAL);
        }
        return authUser;
    }
}
