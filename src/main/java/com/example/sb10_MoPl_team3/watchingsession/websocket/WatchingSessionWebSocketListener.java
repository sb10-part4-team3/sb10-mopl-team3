package com.example.sb10_MoPl_team3.watchingsession.websocket;

import com.example.sb10_MoPl_team3.global.security.AuthUser;
import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionChange;
import com.example.sb10_MoPl_team3.watchingsession.repository.WatchingSessionRedisRepository.PresenceKey;
import com.example.sb10_MoPl_team3.watchingsession.service.WatchingSessionPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.scheduling.annotation.Scheduled;
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
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class WatchingSessionWebSocketListener {

    private static final Pattern WATCH_DESTINATION =
            Pattern.compile("^/sub/contents/([0-9a-fA-F-]{36})/watch$");

    private final WatchingSessionPresenceService presenceService;
    private final WatchingSessionBroadcastPublisher broadcastPublisher;
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
        changes.forEach(change -> publish(change, "subscribe"));
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
            presenceService.leave(disconnected.contentId(), disconnected.watcherId())
                    .forEach(change -> publish(change, "leave"));
        }
    }

    @Scheduled(fixedDelayString = "${watching-session.presence.heartbeat-interval-ms:10000}")
    public void refreshLocalPresences() {
        Set<PresenceKey> presenceKeys = presences.values().stream()
                .distinct()
                .map(presence -> new PresenceKey(presence.contentId(), presence.watcherId()))
                .collect(Collectors.toSet());
        presenceService.refreshAll(presenceKeys)
                .forEach(presence -> log.debug(
                        "시청 세션 heartbeat 갱신 대상이 Redis에 없습니다. contentId={}, watcherId={}",
                        presence.contentId(), presence.watcherId()));
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

    private void publish(WatchingSessionChange change, String trigger) {
        try {
            broadcastPublisher.publish(change);
        } catch (RuntimeException exception) {
            log.warn(
                    "시청 세션 변경 메시지 broadcast 발행에 실패했습니다. trigger={}, type={}, contentId={}, watcherId={}",
                    trigger,
                    change.type(),
                    change.watchingSession().content().id(),
                    change.watchingSession().watcher().userId(),
                    exception
            );
        }
    }

    private record Presence(UUID contentId, UUID watcherId) {
    }

    private record SubscriptionKey(String sessionId, String subscriptionId) {
    }
}
