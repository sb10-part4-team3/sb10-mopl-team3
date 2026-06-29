package com.example.sb10_MoPl_team3.directmessage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.sb10_MoPl_team3.conversation.entity.Conversation;
import com.example.sb10_MoPl_team3.conversation.repository.ConversationRepository;
import com.example.sb10_MoPl_team3.directmessage.entity.DirectMessage;
import com.example.sb10_MoPl_team3.directmessage.repository.DirectMessageRepository;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DirectMessageServiceTest {

    @Mock
    private DirectMessageRepository directMessageRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @InjectMocks
    private DirectMessageService directMessageService;

    @Test
    @DisplayName("대화방 참여자는 과거 쪽지 목록을 커서 응답으로 조회한다")
    void findAll_success() {
        UUID requestUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID conversationId = UUID.fromString("00000000-0000-0000-0000-000000000011");
        User sender = user(requestUserId, "sender@test.com", "발신자");
        User receiver = user(UUID.fromString("00000000-0000-0000-0000-000000000002"), "receiver@test.com", "수신자");
        Conversation conversation = conversation(conversationId, sender, receiver);

        DirectMessage first = directMessage(
            UUID.fromString("00000000-0000-0000-0000-000000000101"),
            conversation,
            sender,
            receiver,
            "첫 번째",
            Instant.parse("2026-06-29T00:00:00Z")
        );
        DirectMessage second = directMessage(
            UUID.fromString("00000000-0000-0000-0000-000000000102"),
            conversation,
            receiver,
            sender,
            "두 번째",
            Instant.parse("2026-06-29T00:01:00Z")
        );
        DirectMessage extra = directMessage(
            UUID.fromString("00000000-0000-0000-0000-000000000103"),
            conversation,
            sender,
            receiver,
            "세 번째",
            Instant.parse("2026-06-29T00:02:00Z")
        );

        given(conversationRepository.findWithUsersById(conversationId))
            .willReturn(Optional.of(conversation));
        given(directMessageRepository.findByConversationIdWithCursorAsc(
            any(UUID.class),
            any(),
            any(),
            any(Pageable.class)
        )).willReturn(List.of(first, second, extra));
        given(directMessageRepository.countByConversationId(conversationId)).willReturn(3L);

        var response = directMessageService.findAll(
            requestUserId,
            conversationId,
            null,
            null,
            2,
            "ASCENDING",
            "createdAt"
        );

        assertThat(response.data()).hasSize(2);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextCursor()).isEqualTo("2026-06-29T00:01:00Z");
        assertThat(response.nextIdAfter()).isEqualTo(second.getId());
        assertThat(response.totalCount()).isEqualTo(3L);
        assertThat(response.data().get(0).content()).isEqualTo("첫 번째");
    }

    @Test
    @DisplayName("쪽지 목록 조회 파라미터를 생략하면 기본 DESCENDING 분기로 조회한다")
    void findAll_defaultDescending() {
        UUID requestUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID conversationId = UUID.fromString("00000000-0000-0000-0000-000000000011");
        User sender = user(requestUserId, "sender@test.com", "발신자");
        User receiver = user(UUID.fromString("00000000-0000-0000-0000-000000000002"), "receiver@test.com", "수신자");
        Conversation conversation = conversation(conversationId, sender, receiver);
        DirectMessage message = directMessage(
            UUID.fromString("00000000-0000-0000-0000-000000000101"),
            conversation,
            sender,
            receiver,
            "기본 정렬",
            Instant.parse("2026-06-29T00:00:00Z")
        );

        given(conversationRepository.findWithUsersById(conversationId))
            .willReturn(Optional.of(conversation));
        given(directMessageRepository.findByConversationIdWithCursorDesc(
            eq(conversationId),
            eq(null),
            eq(null),
            any(Pageable.class)
        )).willReturn(List.of(message));
        given(directMessageRepository.countByConversationId(conversationId)).willReturn(1L);

        var response = directMessageService.findAll(
            requestUserId,
            conversationId,
            null,
            null,
            null,
            null,
            null
        );

        assertThat(response.data()).hasSize(1);
        assertThat(response.sortBy()).isEqualTo("createdAt");
        assertThat(response.sortDirection()).isEqualTo("DESCENDING");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        then(directMessageRepository).should().findByConversationIdWithCursorDesc(
            eq(conversationId),
            eq(null),
            eq(null),
            pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(21);
        then(directMessageRepository).should(never()).findByConversationIdWithCursorAsc(
            any(),
            any(),
            any(),
            any()
        );
    }

    @Test
    @DisplayName("대화방 소속원이 아닌 사용자는 쪽지 목록 조회가 403 예외로 차단된다")
    void findAll_notParticipant() {
        UUID requestUserId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        UUID conversationId = UUID.fromString("00000000-0000-0000-0000-000000000011");
        Conversation conversation = conversation(
            conversationId,
            user(UUID.fromString("00000000-0000-0000-0000-000000000001"), "a@test.com", "A"),
            user(UUID.fromString("00000000-0000-0000-0000-000000000002"), "b@test.com", "B")
        );

        given(conversationRepository.findWithUsersById(conversationId))
            .willReturn(Optional.of(conversation));

        assertThatThrownBy(() -> directMessageService.findAll(
            requestUserId,
            conversationId,
            null,
            null,
            20,
            "DESCENDING",
            "createdAt"
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED)
        );
    }

    @Test
    @DisplayName("대화방이 없으면 쪽지 목록 조회에서 대화방 없음 예외를 던진다")
    void findAll_conversationNotFound() {
        UUID requestUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID conversationId = UUID.fromString("00000000-0000-0000-0000-000000000011");

        given(conversationRepository.findWithUsersById(conversationId))
            .willReturn(Optional.empty());

        assertThatThrownBy(() -> directMessageService.findAll(
            requestUserId,
            conversationId,
            null,
            null,
            20,
            "DESCENDING",
            "createdAt"
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONVERSATION_NOT_FOUND)
        );
    }

    @Test
    @DisplayName("쪽지 목록 조회에서 커서와 보조 커서가 함께 전달되지 않으면 커서 예외를 던진다")
    void findAll_invalidCursorPair() {
        UUID requestUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID conversationId = UUID.fromString("00000000-0000-0000-0000-000000000011");
        User requestUser = user(requestUserId, "a@test.com", "A");
        Conversation conversation = conversation(
            conversationId,
            requestUser,
            user(UUID.fromString("00000000-0000-0000-0000-000000000002"), "b@test.com", "B")
        );

        given(conversationRepository.findWithUsersById(conversationId))
            .willReturn(Optional.of(conversation));

        assertThatThrownBy(() -> directMessageService.findAll(
            requestUserId,
            conversationId,
            "2026-06-29T00:00:00Z",
            null,
            20,
            "DESCENDING",
            "createdAt"
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_CURSOR)
        );
    }

    @Test
    @DisplayName("쪽지 목록 조회 정렬 방향이 올바르지 않으면 정렬 예외를 던진다")
    void findAll_invalidSortDirection() {
        UUID requestUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID conversationId = UUID.fromString("00000000-0000-0000-0000-000000000011");
        User requestUser = user(requestUserId, "a@test.com", "A");
        Conversation conversation = conversation(
            conversationId,
            requestUser,
            user(UUID.fromString("00000000-0000-0000-0000-000000000002"), "b@test.com", "B")
        );

        given(conversationRepository.findWithUsersById(conversationId))
            .willReturn(Optional.of(conversation));

        assertThatThrownBy(() -> directMessageService.findAll(
            requestUserId,
            conversationId,
            null,
            null,
            20,
            "INVALID",
            "createdAt"
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_SORT_DIRECTION)
        );
    }

    @Test
    @DisplayName("쪽지 목록 조회 정렬 기준이 올바르지 않으면 입력값 예외를 던진다")
    void findAll_invalidSortBy() {
        UUID requestUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID conversationId = UUID.fromString("00000000-0000-0000-0000-000000000011");
        User requestUser = user(requestUserId, "a@test.com", "A");
        Conversation conversation = conversation(
            conversationId,
            requestUser,
            user(UUID.fromString("00000000-0000-0000-0000-000000000002"), "b@test.com", "B")
        );

        given(conversationRepository.findWithUsersById(conversationId))
            .willReturn(Optional.of(conversation));

        assertThatThrownBy(() -> directMessageService.findAll(
            requestUserId,
            conversationId,
            null,
            null,
            20,
            "DESCENDING",
            "updatedAt"
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE)
        );
    }

    private User user(UUID id, String email, String name) {
        User user = new User(email, name, "password", null, UserRole.USER);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Conversation conversation(UUID id, User user1, User user2) {
        Conversation conversation = new Conversation(user1, user2);
        ReflectionTestUtils.setField(conversation, "id", id);
        ReflectionTestUtils.setField(conversation, "createdAt", Instant.parse("2026-06-29T00:00:00Z"));
        return conversation;
    }

    private DirectMessage directMessage(
        UUID id,
        Conversation conversation,
        User sender,
        User receiver,
        String content,
        Instant createdAt
    ) {
        DirectMessage directMessage = new DirectMessage(conversation, sender, receiver, content);
        ReflectionTestUtils.setField(directMessage, "id", id);
        ReflectionTestUtils.setField(directMessage, "createdAt", createdAt);
        return directMessage;
    }
}
