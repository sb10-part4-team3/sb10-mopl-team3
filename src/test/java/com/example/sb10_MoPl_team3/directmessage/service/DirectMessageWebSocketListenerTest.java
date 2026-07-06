package com.example.sb10_MoPl_team3.directmessage.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.example.sb10_MoPl_team3.global.security.AuthUser;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@ExtendWith(MockitoExtension.class)
class DirectMessageWebSocketListenerTest {

    @Mock DirectMessageConversationPresence presence;
    @InjectMocks DirectMessageWebSocketListener listener;

    @Test
    void handleSubscribe_tracksAuthenticatedConversationSubscription() {
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        AuthUser authUser = new AuthUser(userId, UserRole.USER, UUID.randomUUID());
        var authentication = new UsernamePasswordAuthenticationToken(
                authUser, null, authUser.authorities());
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setSessionId("session-id");
        accessor.setSubscriptionId("subscription-id");
        accessor.setDestination(
                "/sub/conversations/" + conversationId + "/direct-messages");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        listener.handleSubscribe(new SessionSubscribeEvent(this, message, authentication));

        then(presence).should().subscribe(
                "session-id", "subscription-id", userId, conversationId);
    }

    @Test
    void handleSubscribe_ignoresInvalidDestinationAndUnauthenticatedPrincipal() {
        StompHeaderAccessor invalidDestination =
                StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        invalidDestination.setSessionId("session-id");
        invalidDestination.setSubscriptionId("subscription-id");
        invalidDestination.setDestination("/sub/other-destination");
        Message<byte[]> invalidDestinationMessage = MessageBuilder.createMessage(
                new byte[0], invalidDestination.getMessageHeaders());

        listener.handleSubscribe(new SessionSubscribeEvent(this, invalidDestinationMessage));

        StompHeaderAccessor unauthenticated =
                StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        unauthenticated.setSessionId("session-id");
        unauthenticated.setSubscriptionId("subscription-id");
        unauthenticated.setDestination(
                "/sub/conversations/" + UUID.randomUUID() + "/direct-messages");
        Message<byte[]> unauthenticatedMessage = MessageBuilder.createMessage(
                new byte[0], unauthenticated.getMessageHeaders());
        var invalidAuthentication = new UsernamePasswordAuthenticationToken(
                "invalid-principal", null, java.util.List.of());

        listener.handleSubscribe(new SessionSubscribeEvent(
                this, unauthenticatedMessage, invalidAuthentication));

        then(presence).shouldHaveNoInteractions();
    }

    @Test
    void handleSubscribe_ignoresMissingSessionOrSubscriptionId() {
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        AuthUser authUser = new AuthUser(userId, UserRole.USER, UUID.randomUUID());
        var authentication = new UsernamePasswordAuthenticationToken(
                authUser, null, authUser.authorities());

        StompHeaderAccessor missingSession =
                StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        missingSession.setSubscriptionId("subscription-id");
        missingSession.setDestination(
                "/sub/conversations/" + conversationId + "/direct-messages");
        listener.handleSubscribe(new SessionSubscribeEvent(
                this,
                MessageBuilder.createMessage(new byte[0], missingSession.getMessageHeaders()),
                authentication));

        StompHeaderAccessor missingSubscription =
                StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        missingSubscription.setSessionId("session-id");
        missingSubscription.setDestination(
                "/sub/conversations/" + conversationId + "/direct-messages");
        listener.handleSubscribe(new SessionSubscribeEvent(
                this,
                MessageBuilder.createMessage(
                        new byte[0], missingSubscription.getMessageHeaders()),
                authentication));

        then(presence).shouldHaveNoInteractions();
    }

    @Test
    void handleUnsubscribeAndDisconnect_removeTrackedSubscriptions() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
        accessor.setSessionId("session-id");
        accessor.setSubscriptionId("subscription-id");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        listener.handleUnsubscribe(new SessionUnsubscribeEvent(this, message));
        SessionDisconnectEvent disconnectEvent = mock(SessionDisconnectEvent.class);
        given(disconnectEvent.getSessionId()).willReturn("session-id");
        listener.handleDisconnect(disconnectEvent);

        then(presence).should().unsubscribe("session-id", "subscription-id");
        then(presence).should().disconnect("session-id");
    }

    @Test
    void handleUnsubscribe_ignoresMissingIdentifiers() {
        StompHeaderAccessor missingSession =
                StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
        missingSession.setSubscriptionId("subscription-id");
        listener.handleUnsubscribe(new SessionUnsubscribeEvent(
                this,
                MessageBuilder.createMessage(new byte[0], missingSession.getMessageHeaders())));

        StompHeaderAccessor missingSubscription =
                StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
        missingSubscription.setSessionId("session-id");
        listener.handleUnsubscribe(new SessionUnsubscribeEvent(
                this,
                MessageBuilder.createMessage(
                        new byte[0], missingSubscription.getMessageHeaders())));

        then(presence).shouldHaveNoInteractions();
    }
}
