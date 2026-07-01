package com.example.sb10_MoPl_team3.directmessage.controller;

import com.example.sb10_MoPl_team3.directmessage.dto.DirectMessageDto;
import com.example.sb10_MoPl_team3.directmessage.dto.DirectMessageSendRequest;
import com.example.sb10_MoPl_team3.directmessage.service.DirectMessageAsyncService;
import com.example.sb10_MoPl_team3.global.security.AuthUser;
import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class DirectMessageWebSocketControllerTest {

    @Mock DirectMessageAsyncService asyncService;
    @Mock SimpMessagingTemplate messagingTemplate;
    @InjectMocks DirectMessageWebSocketController controller;

    @Test
    @DisplayName("비동기 저장 완료 후 대화방 구독 경로로 쪽지를 전송한다")
    void send_broadcastsOnlyAfterAsyncSaveCompletes() {
        UUID conversationId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        AuthUser authUser = new AuthUser(senderId, UserRole.USER, null);
        var authentication = new UsernamePasswordAuthenticationToken(
                authUser, null, authUser.authorities());
        CompletableFuture<DirectMessageDto> pending = new CompletableFuture<>();
        given(asyncService.saveAsync(conversationId, senderId, "메시지")).willReturn(pending);

        CompletableFuture<Void> result = controller.send(
                conversationId, new DirectMessageSendRequest("메시지"), authentication);
        then(messagingTemplate).shouldHaveNoInteractions();

        DirectMessageDto message = new DirectMessageDto(
                UUID.randomUUID(), conversationId, Instant.now(),
                new UserSummary(senderId, "발신자", null),
                new UserSummary(receiverId, "수신자", null), "메시지");
        pending.complete(message);
        result.join();

        then(messagingTemplate).should().convertAndSend(
                "/sub/conversations/" + conversationId + "/direct-messages", message);
    }

    @Test
    @DisplayName("인증 정보가 없으면 쪽지 전송을 거부한다")
    void send_rejectsUnauthenticatedRequest() {
        assertThatThrownBy(() -> controller.send(
                UUID.randomUUID(), new DirectMessageSendRequest("메시지"), null))
                .isInstanceOf(MessagingException.class);
        then(asyncService).shouldHaveNoInteractions();
        then(messagingTemplate).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("인증 principal이 AuthUser가 아니면 쪽지 전송을 거부한다")
    void send_rejectsInvalidPrincipal() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "invalid-principal", null, java.util.List.of());

        assertThatThrownBy(() -> controller.send(
                UUID.randomUUID(), new DirectMessageSendRequest("메시지"), authentication))
                .isInstanceOf(MessagingException.class);
        then(asyncService).shouldHaveNoInteractions();
        then(messagingTemplate).shouldHaveNoInteractions();
    }
}
