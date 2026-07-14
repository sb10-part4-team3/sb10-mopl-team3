package com.example.sb10_MoPl_team3.directmessage.service;

import com.example.sb10_MoPl_team3.conversation.entity.Conversation;
import com.example.sb10_MoPl_team3.conversation.repository.ConversationRepository;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.global.security.AuthUser;
import java.security.Principal;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Component
@RequiredArgsConstructor
public class DirectMessageWebSocketListener {

    private static final Pattern DIRECT_MESSAGE_DESTINATION = Pattern.compile(
            "^/sub/conversations/([0-9a-fA-F-]{36})/direct-messages$");

    private final DirectMessageConversationPresence presence;
    private final ConversationRepository conversationRepository;

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        UUID conversationId = parseConversationId(accessor.getDestination());
        AuthUser authUser = extractAuthUser(event.getUser());
        if (conversationId == null || authUser == null
                || accessor.getSessionId() == null || accessor.getSubscriptionId() == null) {
            return;
        }
        validateParticipant(conversationId, authUser.userId());
        presence.subscribe(
                accessor.getSessionId(),
                accessor.getSubscriptionId(),
                authUser.userId(),
                conversationId);
    }

    @EventListener
    public void handleUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (accessor.getSessionId() == null || accessor.getSubscriptionId() == null) {
            return;
        }
        presence.unsubscribe(accessor.getSessionId(), accessor.getSubscriptionId());
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        presence.disconnect(event.getSessionId());
    }

    private UUID parseConversationId(String destination) {
        if (destination == null) {
            return null;
        }
        Matcher matcher = DIRECT_MESSAGE_DESTINATION.matcher(destination);
        if (!matcher.matches()) {
            return null;
        }
        try {
            return UUID.fromString(matcher.group(1));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private AuthUser extractAuthUser(Principal principal) {
        if (principal instanceof org.springframework.security.core.Authentication authentication
                && authentication.getPrincipal() instanceof AuthUser authUser) {
            return authUser;
        }
        return null;
    }

    private void validateParticipant(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findWithUsersById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));
        if (!conversation.getUser1().getId().equals(userId)
                && !conversation.getUser2().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }
}
