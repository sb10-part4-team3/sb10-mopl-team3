package com.example.sb10_MoPl_team3.directmessage.service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class DirectMessageConversationPresence {

    private final Map<SubscriptionKey, ActiveConversation> subscriptions =
            new ConcurrentHashMap<>();
    private final Map<ActiveConversation, Set<SubscriptionKey>> activeSubscriptions =
            new ConcurrentHashMap<>();

    public synchronized void subscribe(
            String sessionId,
            String subscriptionId,
            UUID userId,
            UUID conversationId
    ) {
        SubscriptionKey key = new SubscriptionKey(sessionId, subscriptionId);
        ActiveConversation activeConversation = new ActiveConversation(userId, conversationId);
        ActiveConversation previous = subscriptions.put(key, activeConversation);
        if (previous != null && !previous.equals(activeConversation)) {
            removeFromActiveIndex(previous, key);
        }
        activeSubscriptions.compute(activeConversation, (ignored, keys) -> {
            Set<SubscriptionKey> indexedKeys =
                    keys == null ? ConcurrentHashMap.newKeySet() : keys;
            indexedKeys.add(key);
            return indexedKeys;
        });
    }

    public synchronized void unsubscribe(String sessionId, String subscriptionId) {
        SubscriptionKey key = new SubscriptionKey(sessionId, subscriptionId);
        ActiveConversation removed = subscriptions.remove(key);
        if (removed != null) {
            removeFromActiveIndex(removed, key);
        }
    }

    public synchronized void disconnect(String sessionId) {
        subscriptions.forEach((key, activeConversation) -> {
            if (key.sessionId().equals(sessionId)
                    && subscriptions.remove(key, activeConversation)) {
                removeFromActiveIndex(activeConversation, key);
            }
        });
    }

    public synchronized boolean isActive(UUID userId, UUID conversationId) {
        Set<SubscriptionKey> keys = activeSubscriptions.get(
                new ActiveConversation(userId, conversationId));
        return keys != null && !keys.isEmpty();
    }

    private void removeFromActiveIndex(
            ActiveConversation activeConversation,
            SubscriptionKey key
    ) {
        activeSubscriptions.computeIfPresent(activeConversation, (ignored, keys) -> {
            keys.remove(key);
            return keys.isEmpty() ? null : keys;
        });
    }

    private record SubscriptionKey(String sessionId, String subscriptionId) {
    }

    private record ActiveConversation(UUID userId, UUID conversationId) {
    }
}
