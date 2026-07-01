package com.example.sb10_MoPl_team3.directmessage.service;

import com.example.sb10_MoPl_team3.conversation.entity.Conversation;
import com.example.sb10_MoPl_team3.conversation.repository.ConversationRepository;
import com.example.sb10_MoPl_team3.directmessage.entity.DirectMessage;
import com.example.sb10_MoPl_team3.directmessage.repository.DirectMessageRepository;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class DirectMessageAsyncServiceTest {

    @Mock DirectMessageRepository directMessageRepository;
    @Mock ConversationRepository conversationRepository;
    @InjectMocks DirectMessageAsyncService service;

    @Test
    @DisplayName("대화 참여자가 보낸 쪽지를 저장하고 상대 참여자를 수신자로 지정한다")
    void saveAsync_persistsMessageAndDeterminesReceiver() {
        UUID conversationId = UUID.randomUUID();
        UUID senderId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        User sender = user(senderId, "발신자");
        User receiver = user(UUID.fromString("00000000-0000-0000-0000-000000000002"), "수신자");
        Conversation conversation = new Conversation(sender, receiver);
        ReflectionTestUtils.setField(conversation, "id", conversationId);
        given(conversationRepository.findWithUsersById(conversationId))
                .willReturn(Optional.of(conversation));
        given(directMessageRepository.saveAndFlush(any(DirectMessage.class)))
                .willAnswer(invocation -> {
                    DirectMessage message = invocation.getArgument(0);
                    ReflectionTestUtils.setField(message, "id", UUID.randomUUID());
                    ReflectionTestUtils.setField(message, "createdAt", Instant.parse("2026-07-01T00:00:00Z"));
                    return message;
                });

        var result = service.saveAsync(conversationId, senderId, "안녕하세요").join();

        assertThat(result.conversationId()).isEqualTo(conversationId);
        assertThat(result.sender().userId()).isEqualTo(senderId);
        assertThat(result.receiver().userId()).isEqualTo(receiver.getId());
        assertThat(result.content()).isEqualTo("안녕하세요");
        then(directMessageRepository).should().saveAndFlush(any(DirectMessage.class));
    }

    @Test
    @DisplayName("대화에 참여하지 않은 사용자의 쪽지 전송을 거부한다")
    void saveAsync_rejectsNonParticipant() {
        UUID conversationId = UUID.randomUUID();
        Conversation conversation = new Conversation(
                user(UUID.randomUUID(), "참여자1"), user(UUID.randomUUID(), "참여자2"));
        given(conversationRepository.findWithUsersById(conversationId))
                .willReturn(Optional.of(conversation));

        assertThatThrownBy(() -> service.saveAsync(
                conversationId, UUID.randomUUID(), "메시지"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACCESS_DENIED);
        then(directMessageRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("두 번째 대화 참여자도 발신자로 쪽지를 저장할 수 있다")
    void saveAsync_supportsSecondParticipantAsSender() {
        UUID conversationId = UUID.randomUUID();
        User first = user(UUID.fromString("00000000-0000-0000-0000-000000000001"), "첫 번째");
        User second = user(UUID.fromString("00000000-0000-0000-0000-000000000002"), "두 번째");
        Conversation conversation = new Conversation(first, second);
        ReflectionTestUtils.setField(conversation, "id", conversationId);
        given(conversationRepository.findWithUsersById(conversationId))
                .willReturn(Optional.of(conversation));
        given(directMessageRepository.saveAndFlush(any(DirectMessage.class)))
                .willAnswer(invocation -> {
                    DirectMessage message = invocation.getArgument(0);
                    ReflectionTestUtils.setField(message, "id", UUID.randomUUID());
                    ReflectionTestUtils.setField(message, "createdAt", Instant.now());
                    return message;
                });

        var result = service.saveAsync(conversationId, second.getId(), "답장").join();

        assertThat(result.sender().userId()).isEqualTo(second.getId());
        assertThat(result.receiver().userId()).isEqualTo(first.getId());
    }

    @Test
    @DisplayName("존재하지 않는 대화방이면 CONVERSATION_NOT_FOUND 예외를 던진다")
    void saveAsync_conversationNotFound() {
        UUID conversationId = UUID.randomUUID();
        given(conversationRepository.findWithUsersById(conversationId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.saveAsync(conversationId, UUID.randomUUID(), "메시지"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CONVERSATION_NOT_FOUND);
    }

    private User user(UUID id, String name) {
        User user = new User(id + "@test.com", name, "password", null, UserRole.USER);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
