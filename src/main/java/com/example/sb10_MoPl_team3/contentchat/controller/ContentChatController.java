package com.example.sb10_MoPl_team3.contentchat.controller;

import com.example.sb10_MoPl_team3.contentchat.dto.ContentChatDto;
import com.example.sb10_MoPl_team3.contentchat.dto.ContentChatSendRequest;
import com.example.sb10_MoPl_team3.contentchat.service.ContentChatService;
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

@Controller
@RequiredArgsConstructor
public class ContentChatController {

    private static final String CHAT_DESTINATION_FORMAT = "/sub/contents/%s/chat";

    private final ContentChatService contentChatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/contents/{contentId}/chat")
    public void sendMessage(
            @DestinationVariable UUID contentId,
            @Valid @Payload ContentChatSendRequest request,
            Authentication authentication
    ) {
        AuthUser authUser = extractAuthUser(authentication);
        ContentChatDto message = contentChatService.createMessage(
                contentId,
                authUser.userId(),
                request.content()
        );
        messagingTemplate.convertAndSend(CHAT_DESTINATION_FORMAT.formatted(contentId), message);
    }

    private AuthUser extractAuthUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser authUser)) {
            throw new MessagingException("인증된 사용자만 채팅 메시지를 전송할 수 있습니다.");
        }
        return authUser;
    }
}
