package com.example.sb10_MoPl_team3.directmessage.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class DirectMessageConversationPresence {

    private final Map<SubscriptionKey, ActiveConversation> subscriptions =
            new ConcurrentHashMap<>();

    public void subscribe(
            String sessionId,
            String subscriptionId,
            UUID userId,
            UUID conversationId
    ) {
        subscriptions.put(
                new SubscriptionKey(sessionId, subscriptionId),
                new ActiveConversation(userId, conversationId));
    }

    public void unsubscribe(String sessionId, String subscriptionId) {
        subscriptions.remove(new SubscriptionKey(sessionId, subscriptionId));
    }

    public void disconnect(String sessionId) {
        subscriptions.keySet().removeIf(key -> key.sessionId().equals(sessionId));
    }

    public boolean isActive(UUID userId, UUID conversationId) {
        return subscriptions.containsValue(new ActiveConversation(userId, conversationId));
    }

    private record SubscriptionKey(String sessionId, String subscriptionId) {
    }

    private record ActiveConversation(UUID userId, UUID conversationId) {
    }
}
