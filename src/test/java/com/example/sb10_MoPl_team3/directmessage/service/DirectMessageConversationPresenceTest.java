package com.example.sb10_MoPl_team3.directmessage.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DirectMessageConversationPresenceTest {

    private final DirectMessageConversationPresence presence =
            new DirectMessageConversationPresence();

    @Test
    void tracksMultipleConnectionsAndRemovesOnlyMatchingConnection() {
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        presence.subscribe("session-1", "subscription-1", userId, conversationId);
        presence.subscribe("session-2", "subscription-2", userId, conversationId);

        presence.unsubscribe("session-1", "subscription-1");
        assertThat(presence.isActive(userId, conversationId)).isTrue();

        presence.disconnect("session-2");
        assertThat(presence.isActive(userId, conversationId)).isFalse();
    }

    @Test
    void replacingSubscriptionMovesActiveConversationIndex() {
        UUID userId = UUID.randomUUID();
        UUID previousConversationId = UUID.randomUUID();
        UUID nextConversationId = UUID.randomUUID();
        presence.subscribe(
                "session-1", "subscription-1", userId, previousConversationId);

        presence.subscribe(
                "session-1", "subscription-1", userId, nextConversationId);

        assertThat(presence.isActive(userId, previousConversationId)).isFalse();
        assertThat(presence.isActive(userId, nextConversationId)).isTrue();
    }

    @Test
    void concurrentResubscribeLeavesOnlyOneConversationActive() {
        UUID userId = UUID.randomUUID();
        UUID firstConversationId = UUID.randomUUID();
        UUID secondConversationId = UUID.randomUUID();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CompletableFuture<Void> first = CompletableFuture.runAsync(
                    () -> presence.subscribe(
                            "session-1", "subscription-1", userId, firstConversationId),
                    executor);
            CompletableFuture<Void> second = CompletableFuture.runAsync(
                    () -> presence.subscribe(
                            "session-1", "subscription-1", userId, secondConversationId),
                    executor);

            CompletableFuture.allOf(first, second).join();

            assertThat(presence.isActive(userId, firstConversationId)
                    ^ presence.isActive(userId, secondConversationId)).isTrue();
        } finally {
            executor.shutdownNow();
        }
    }
}
