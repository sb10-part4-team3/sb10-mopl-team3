package com.example.sb10_MoPl_team3.watchingsession.websocket;

import com.example.sb10_MoPl_team3.global.security.AuthUser;
import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionChange;
import com.example.sb10_MoPl_team3.watchingsession.service.WatchingSessionPresenceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class WatchingSessionWebSocketListenerTest {

    @Mock WatchingSessionPresenceService presenceService;
    @Mock SimpMessagingTemplate messagingTemplate;
    @InjectMocks WatchingSessionWebSocketListener listener;

    @Test
    @DisplayName("watch 경로 구독 시 입장 처리 후 변경된 명단을 해당 방에 전송한다")
    void handleSubscribe_publishesWatcherChange() {
        UUID contentId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID watcherId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        AuthUser authUser = new AuthUser(watcherId, UserRole.USER, null);
        var authentication = new UsernamePasswordAuthenticationToken(
                authUser, null, authUser.authorities());
        WatchingSessionChange change = new WatchingSessionChange(
                contentId, List.of(new UserSummary(watcherId, "시청자", null)));
        given(presenceService.join(contentId, watcherId)).willReturn(List.of(change));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/sub/contents/" + contentId + "/watch");
        accessor.setSessionId("session-1");
        accessor.setUser(authentication);
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        SessionSubscribeEvent event = new SessionSubscribeEvent(this, message, authentication);

        listener.handleSubscribe(event);

        then(presenceService).should().join(contentId, watcherId);
        then(messagingTemplate).should().convertAndSend(
                "/sub/contents/" + contentId + "/watch", change);
    }

    @Test
    @DisplayName("watch 경로가 아닌 구독은 시청 명단을 변경하지 않는다")
    void handleSubscribe_ignoresOtherDestination() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/sub/contents/other/chat");
        accessor.setSessionId("session-1");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        listener.handleSubscribe(new SessionSubscribeEvent(this, message));

        then(presenceService).shouldHaveNoInteractions();
        then(messagingTemplate).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("인증되지 않은 watch 구독은 시청 명단을 변경하지 않는다")
    void handleSubscribe_ignoresUnauthenticatedUser() {
        UUID contentId = UUID.randomUUID();
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/sub/contents/" + contentId + "/watch");
        accessor.setSessionId("session-1");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        listener.handleSubscribe(new SessionSubscribeEvent(this, message));

        then(presenceService).shouldHaveNoInteractions();
        then(messagingTemplate).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("등록되지 않은 연결 종료와 구독 해제는 무시한다")
    void unknownConnectionEvents_areIgnored() {
        SessionDisconnectEvent disconnectEvent = mock(SessionDisconnectEvent.class);
        listener.handleDisconnect(disconnectEvent);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
        accessor.setSessionId("unknown-session");
        accessor.setSubscriptionId("unknown-subscription");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        listener.handleUnsubscribe(new SessionUnsubscribeEvent(this, message));

        then(presenceService).shouldHaveNoInteractions();
        then(messagingTemplate).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("같은 사용자의 구독이 여러 개면 마지막 구독 해제 시에만 퇴장 처리한다")
    void multipleSubscriptions_leaveOnlyAfterLastUnsubscribe() {
        UUID contentId = UUID.randomUUID();
        UUID watcherId = UUID.randomUUID();
        AuthUser authUser = new AuthUser(watcherId, UserRole.USER, null);
        var authentication = new UsernamePasswordAuthenticationToken(
                authUser, null, authUser.authorities());
        WatchingSessionChange joined = new WatchingSessionChange(contentId, List.of());
        WatchingSessionChange left = new WatchingSessionChange(contentId, List.of());
        given(presenceService.join(contentId, watcherId)).willReturn(List.of(joined));
        given(presenceService.leave(contentId, watcherId)).willReturn(left);

        subscribe(contentId, authentication, "session-1", "subscription-1");
        subscribe(contentId, authentication, "session-2", "subscription-2");
        unsubscribe("session-1", "subscription-1", authentication);

        then(presenceService).should(never()).leave(contentId, watcherId);

        unsubscribe("session-2", "subscription-2", authentication);

        then(presenceService).should().leave(contentId, watcherId);
    }

    @Test
    @DisplayName("세션 ID가 없는 구독 해제와 잘못된 UUID watch 경로는 무시한다")
    void invalidEvents_areIgnored() {
        StompHeaderAccessor invalidSubscribe = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        invalidSubscribe.setDestination("/sub/contents/------------------------------------/watch");
        invalidSubscribe.setSessionId("session-1");
        invalidSubscribe.setLeaveMutable(true);
        Message<byte[]> subscribeMessage = MessageBuilder.createMessage(
                new byte[0], invalidSubscribe.getMessageHeaders());
        listener.handleSubscribe(new SessionSubscribeEvent(this, subscribeMessage));

        StompHeaderAccessor invalidUnsubscribe = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
        invalidUnsubscribe.setSubscriptionId("subscription-1");
        invalidUnsubscribe.setLeaveMutable(true);
        Message<byte[]> unsubscribeMessage = MessageBuilder.createMessage(
                new byte[0], invalidUnsubscribe.getMessageHeaders());
        listener.handleUnsubscribe(new SessionUnsubscribeEvent(this, unsubscribeMessage));

        then(presenceService).shouldHaveNoInteractions();
        then(messagingTemplate).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("구독 중인 WebSocket 연결이 해제되면 퇴장 처리 후 변경 명단을 전송한다")
    void handleDisconnect_publishesWatcherChange() {
        UUID contentId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID watcherId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        AuthUser authUser = new AuthUser(watcherId, UserRole.USER, null);
        var authentication = new UsernamePasswordAuthenticationToken(
                authUser, null, authUser.authorities());
        WatchingSessionChange joined = new WatchingSessionChange(
                contentId, List.of(new UserSummary(watcherId, "시청자", null)));
        WatchingSessionChange left = new WatchingSessionChange(contentId, List.of());
        given(presenceService.join(contentId, watcherId)).willReturn(List.of(joined));
        given(presenceService.leave(contentId, watcherId)).willReturn(left);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/sub/contents/" + contentId + "/watch");
        accessor.setSessionId("session-1");
        accessor.setUser(authentication);
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        listener.handleSubscribe(new SessionSubscribeEvent(this, message, authentication));

        SessionDisconnectEvent disconnectEvent = mock(SessionDisconnectEvent.class);
        given(disconnectEvent.getSessionId()).willReturn("session-1");
        listener.handleDisconnect(disconnectEvent);

        then(presenceService).should().leave(contentId, watcherId);
        then(messagingTemplate).should().convertAndSend(
                "/sub/contents/" + contentId + "/watch", left);
    }

    @Test
    @DisplayName("watch 경로 구독을 해제하면 퇴장 처리 후 변경 명단을 전송한다")
    void handleUnsubscribe_publishesWatcherChange() {
        UUID contentId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID watcherId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        AuthUser authUser = new AuthUser(watcherId, UserRole.USER, null);
        var authentication = new UsernamePasswordAuthenticationToken(
                authUser, null, authUser.authorities());
        WatchingSessionChange joined = new WatchingSessionChange(
                contentId, List.of(new UserSummary(watcherId, "시청자", null)));
        WatchingSessionChange left = new WatchingSessionChange(contentId, List.of());
        given(presenceService.join(contentId, watcherId)).willReturn(List.of(joined));
        given(presenceService.leave(contentId, watcherId)).willReturn(left);

        StompHeaderAccessor subscribe = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subscribe.setDestination("/sub/contents/" + contentId + "/watch");
        subscribe.setSessionId("session-1");
        subscribe.setSubscriptionId("watch-subscription");
        subscribe.setUser(authentication);
        subscribe.setLeaveMutable(true);
        Message<byte[]> subscribeMessage = MessageBuilder.createMessage(
                new byte[0], subscribe.getMessageHeaders());
        listener.handleSubscribe(new SessionSubscribeEvent(this, subscribeMessage, authentication));

        StompHeaderAccessor unsubscribe = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
        unsubscribe.setSessionId("session-1");
        unsubscribe.setSubscriptionId("watch-subscription");
        unsubscribe.setLeaveMutable(true);
        Message<byte[]> unsubscribeMessage = MessageBuilder.createMessage(
                new byte[0], unsubscribe.getMessageHeaders());
        listener.handleUnsubscribe(new SessionUnsubscribeEvent(this, unsubscribeMessage, authentication));

        then(presenceService).should().leave(contentId, watcherId);
        then(messagingTemplate).should().convertAndSend(
                "/sub/contents/" + contentId + "/watch", left);
    }

    private void subscribe(
            UUID contentId,
            UsernamePasswordAuthenticationToken authentication,
            String sessionId,
            String subscriptionId
    ) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/sub/contents/" + contentId + "/watch");
        accessor.setSessionId(sessionId);
        accessor.setSubscriptionId(subscriptionId);
        accessor.setUser(authentication);
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        listener.handleSubscribe(new SessionSubscribeEvent(this, message, authentication));
    }

    private void unsubscribe(
            String sessionId,
            String subscriptionId,
            UsernamePasswordAuthenticationToken authentication
    ) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
        accessor.setSessionId(sessionId);
        accessor.setSubscriptionId(subscriptionId);
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        listener.handleUnsubscribe(new SessionUnsubscribeEvent(this, message, authentication));
    }
}
