package com.example.sb10_MoPl_team3.watchingsession.websocket;

import com.example.sb10_MoPl_team3.global.security.AuthUser;
import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionChange;
import com.example.sb10_MoPl_team3.watchingsession.service.WatchingSessionPresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.security.Principal;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class WatchingSessionWebSocketListener {

    private static final Pattern WATCH_DESTINATION =
            Pattern.compile("^/sub/contents/([0-9a-fA-F-]{36})/watch$");
    private static final String WATCH_DESTINATION_FORMAT = "/sub/contents/%s/watch";

    private final WatchingSessionPresenceService presenceService;
    private final SimpMessagingTemplate messagingTemplate;
    private final Map<SubscriptionKey, Presence> presences = new ConcurrentHashMap<>();

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        UUID contentId = parseContentId(accessor.getDestination());
        AuthUser authUser = extractAuthUser(event.getUser());
        if (contentId == null || authUser == null || accessor.getSessionId() == null) {
            return;
        }

        SubscriptionKey key = new SubscriptionKey(
                accessor.getSessionId(), accessor.getSubscriptionId());
        var changes = presenceService.join(contentId, authUser.userId());
        presences.put(key, new Presence(contentId, authUser.userId()));
        changes.forEach(this::publish);
    }

    @EventListener
    public void handleUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (accessor.getSessionId() == null) {
            return;
        }
        removePresence(new SubscriptionKey(
                accessor.getSessionId(), accessor.getSubscriptionId()));
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        Set<Presence> disconnected = new HashSet<>();
        presences.entrySet().removeIf(entry -> {
            if (entry.getKey().sessionId().equals(event.getSessionId())) {
                disconnected.add(entry.getValue());
                return true;
            }
            return false;
        });
        disconnected.forEach(this::leaveIfLastConnection);
    }

    private void removePresence(SubscriptionKey key) {
        Presence disconnected = presences.remove(key);
        if (disconnected != null) {
            leaveIfLastConnection(disconnected);
        }
    }

    private void leaveIfLastConnection(Presence disconnected) {
        if (!presences.containsValue(disconnected)) {
            publish(presenceService.leave(disconnected.contentId(), disconnected.watcherId()));
        }
    }

    private UUID parseContentId(String destination) {
        if (destination == null) {
            return null;
        }
        Matcher matcher = WATCH_DESTINATION.matcher(destination);
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

    private void publish(WatchingSessionChange change) {
        messagingTemplate.convertAndSend(
                WATCH_DESTINATION_FORMAT.formatted(change.contentId()),
                change
        );
    }

    private record Presence(UUID contentId, UUID watcherId) {
    }

    private record SubscriptionKey(String sessionId, String subscriptionId) {
    }
}
