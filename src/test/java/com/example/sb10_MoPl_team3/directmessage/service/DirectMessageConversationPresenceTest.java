package com.example.sb10_MoPl_team3.directmessage.service;

import static org.assertj.core.api.Assertions.assertThat;

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
}
