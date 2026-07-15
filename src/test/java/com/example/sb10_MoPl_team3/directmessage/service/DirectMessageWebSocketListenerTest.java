package com.example.sb10_MoPl_team3.directmessage.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.example.sb10_MoPl_team3.conversation.entity.Conversation;
import com.example.sb10_MoPl_team3.conversation.repository.ConversationRepository;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.global.security.AuthUser;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@ExtendWith(MockitoExtension.class)
class DirectMessageWebSocketListenerTest {

    @Mock DirectMessageConversationPresence presence;
    @Mock ConversationRepository conversationRepository;
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
        given(conversationRepository.findWithUsersById(conversationId))
                .willReturn(Optional.of(conversation(conversationId, userId, UUID.randomUUID())));

        listener.handleSubscribe(new SessionSubscribeEvent(this, message, authentication));

        then(presence).should().subscribe(
                "session-id", "subscription-id", userId, conversationId);
    }

    @Test
    @DisplayName("대화 참여자가 아니면 DM 구독을 등록하지 않는다")
    void handleSubscribe_rejectsNonParticipant() {
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
        given(conversationRepository.findWithUsersById(conversationId))
                .willReturn(Optional.of(conversation(conversationId, UUID.randomUUID(), UUID.randomUUID())));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> listener.handleSubscribe(
                new SessionSubscribeEvent(this, message, authentication)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCESS_DENIED);

        then(presence).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("존재하지 않는 대화방은 DM 구독을 등록하지 않는다")
    void handleSubscribe_rejectsMissingConversation() {
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
        given(conversationRepository.findWithUsersById(conversationId)).willReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> listener.handleSubscribe(
                new SessionSubscribeEvent(this, message, authentication)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CONVERSATION_NOT_FOUND);

        then(presence).shouldHaveNoInteractions();
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

    private Conversation conversation(UUID conversationId, UUID firstUserId, UUID secondUserId) {
        User firstUser = new User(firstUserId + "@test.com", "첫 사용자", "password", null, UserRole.USER);
        User secondUser = new User(secondUserId + "@test.com", "두 번째 사용자", "password", null, UserRole.USER);
        ReflectionTestUtils.setField(firstUser, "id", firstUserId);
        ReflectionTestUtils.setField(secondUser, "id", secondUserId);
        Conversation conversation = new Conversation(firstUser, secondUser);
        ReflectionTestUtils.setField(conversation, "id", conversationId);
        return conversation;
    }
}
