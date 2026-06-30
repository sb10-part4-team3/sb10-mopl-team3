package com.example.sb10_MoPl_team3.contentchat.controller;

import com.example.sb10_MoPl_team3.contentchat.dto.ContentChatDto;
import com.example.sb10_MoPl_team3.contentchat.dto.ContentChatSendRequest;
import com.example.sb10_MoPl_team3.contentchat.service.ContentChatService;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.global.security.AuthUser;
import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ContentChatControllerTest {

    @Mock
    private ContentChatService contentChatService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ContentChatController contentChatController;

    @Test
    @DisplayName("수신한 채팅을 해당 콘텐츠 구독 경로로 즉시 전달한다")
    void sendMessage_success() {
        UUID contentId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID senderId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID messageId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        AuthUser authUser = new AuthUser(senderId, UserRole.USER, null);
        var authentication = new UsernamePasswordAuthenticationToken(
                authUser, null, authUser.authorities());
        ContentChatDto message = new ContentChatDto(
                messageId,
                contentId,
                Instant.parse("2026-06-30T00:00:00Z"),
                new UserSummary(senderId, "시청자", null),
                "같이 봐요"
        );
        given(contentChatService.createMessage(contentId, senderId, "같이 봐요"))
                .willReturn(message);

        contentChatController.sendMessage(
                contentId, new ContentChatSendRequest("같이 봐요"), authentication);

        ArgumentCaptor<ContentChatDto> messageCaptor = ArgumentCaptor.forClass(ContentChatDto.class);
        then(messagingTemplate).should().convertAndSend(
                org.mockito.ArgumentMatchers.eq("/sub/contents/" + contentId + "/chat"),
                messageCaptor.capture()
        );
        assertThat(messageCaptor.getValue()).isEqualTo(message);
    }

    @Test
    @DisplayName("인증 정보가 없으면 채팅 메시지를 전달하지 않는다")
    void sendMessage_unauthenticated() {
        UUID contentId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        assertThatThrownBy(() -> contentChatController.sendMessage(
                contentId, new ContentChatSendRequest("메시지"), null))
                .isInstanceOf(MessagingException.class);
        then(contentChatService).shouldHaveNoInteractions();
        then(messagingTemplate).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("인증 principal이 AuthUser가 아니면 채팅 메시지를 전달하지 않는다")
    void sendMessage_invalidPrincipal() {
        UUID contentId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var authentication = new UsernamePasswordAuthenticationToken(
                "invalid-principal", null, java.util.List.of());

        assertThatThrownBy(() -> contentChatController.sendMessage(
                contentId, new ContentChatSendRequest("메시지"), authentication))
                .isInstanceOf(MessagingException.class);
        then(contentChatService).shouldHaveNoInteractions();
        then(messagingTemplate).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("메시지 생성에 실패하면 구독 경로로 전달하지 않는다")
    void sendMessage_serviceFailure() {
        UUID contentId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID senderId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        AuthUser authUser = new AuthUser(senderId, UserRole.USER, null);
        var authentication = new UsernamePasswordAuthenticationToken(
                authUser, null, authUser.authorities());
        given(contentChatService.createMessage(contentId, senderId, "메시지"))
                .willThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

        assertThatThrownBy(() -> contentChatController.sendMessage(
                contentId, new ContentChatSendRequest("메시지"), authentication))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
        then(messagingTemplate).shouldHaveNoInteractions();
    }
}
