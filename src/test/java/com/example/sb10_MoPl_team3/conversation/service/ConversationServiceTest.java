package com.example.sb10_MoPl_team3.conversation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.sb10_MoPl_team3.conversation.dto.request.ConversationCreateRequest;
import com.example.sb10_MoPl_team3.conversation.dto.request.ConversationFindAllRequest;
import com.example.sb10_MoPl_team3.conversation.entity.Conversation;
import com.example.sb10_MoPl_team3.conversation.repository.ConversationRepository;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
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
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private ConversationService conversationService;

    @Test
    @DisplayName("본인이 참여한 대화방 목록을 커서 응답으로 반환한다")
    void findAll_success() {
        UUID requestUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        User requestUser = user(requestUserId, "me@test.com", "나");
        User otherUser = user(UUID.fromString("00000000-0000-0000-0000-000000000002"), "you@test.com", "상대");

        Conversation first = conversation(
            UUID.fromString("00000000-0000-0000-0000-000000000011"),
            requestUser,
            otherUser,
            Instant.parse("2026-06-29T00:00:00Z")
        );
        Conversation second = conversation(
            UUID.fromString("00000000-0000-0000-0000-000000000012"),
            requestUser,
            otherUser,
            Instant.parse("2026-06-29T00:01:00Z")
        );
        Conversation extra = conversation(
            UUID.fromString("00000000-0000-0000-0000-000000000013"),
            requestUser,
            otherUser,
            Instant.parse("2026-06-29T00:02:00Z")
        );

        ConversationFindAllRequest request = new ConversationFindAllRequest(
            "상대",
            "2026-06-29T00:00:00Z",
            first.getId(),
            2,
            "ASCENDING",
            "createdAt"
        );

        given(conversationRepository.findParticipatingConversationsAsc(
            eq(requestUserId),
            eq("상대"),
            eq(Instant.parse("2026-06-29T00:00:00Z")),
            eq(first.getId()),
            any(Pageable.class)
        )).willReturn(List.of(first, second, extra));
        given(conversationRepository.countParticipatingConversations(requestUserId, "상대"))
            .willReturn(3L);

        var response = conversationService.findAll(requestUserId, request);

        assertThat(response.data()).hasSize(2);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextCursor()).isEqualTo("2026-06-29T00:01:00Z");
        assertThat(response.nextIdAfter()).isEqualTo(second.getId());
        assertThat(response.totalCount()).isEqualTo(3L);
        assertThat(response.sortBy()).isEqualTo("createdAt");
        assertThat(response.sortDirection()).isEqualTo("ASCENDING");
        assertThat(response.data().get(0).with().userId()).isEqualTo(otherUser.getId());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        then(conversationRepository).should().findParticipatingConversationsAsc(
            eq(requestUserId),
            eq("상대"),
            eq(Instant.parse("2026-06-29T00:00:00Z")),
            eq(first.getId()),
            pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(3);
        then(conversationRepository).should(never()).findParticipatingConversationsDesc(
            any(),
            any(),
            any(),
            any(),
            any()
        );
    }

    @Test
    @DisplayName("이미 개설된 대화방이 있으면 신규 저장 없이 기존 대화방을 반환한다")
    void create_existingConversation() {
        UUID requestUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID withUserId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        User requestUser = user(requestUserId, "me@test.com", "나");
        User withUser = user(withUserId, "you@test.com", "상대");
        Conversation existing = conversation(
            UUID.fromString("00000000-0000-0000-0000-000000000011"),
            requestUser,
            withUser,
            Instant.parse("2026-06-29T00:00:00Z")
        );

        given(conversationRepository.findByUserIds(requestUserId, withUserId))
            .willReturn(Optional.of(existing));

        var response = conversationService.create(
            requestUserId,
            new ConversationCreateRequest(withUserId)
        );

        assertThat(response.id()).isEqualTo(existing.getId());
        assertThat(response.with().userId()).isEqualTo(withUserId);
        then(conversationRepository).should(never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("본인 자신과 대화방 생성을 요청하면 입력값 예외를 던진다")
    void create_selfConversation() {
        UUID requestUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        assertThatThrownBy(() -> conversationService.create(
            requestUserId,
            new ConversationCreateRequest(requestUserId)
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE)
        );
    }

    @Test
    @DisplayName("대화방 목록 조회에서 커서와 보조 커서가 함께 전달되지 않으면 커서 예외를 던진다")
    void findAll_invalidCursorPair() {
        UUID requestUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        ConversationFindAllRequest request = new ConversationFindAllRequest(
            null,
            "2026-06-29T00:00:00Z",
            null,
            20,
            "DESCENDING",
            "createdAt"
        );

        assertThatThrownBy(() -> conversationService.findAll(requestUserId, request))
            .isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_CURSOR)
            );
    }

    @Test
    @DisplayName("대화방 목록 조회 정렬 방향이 올바르지 않으면 정렬 예외를 던진다")
    void findAll_invalidSortDirection() {
        UUID requestUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        ConversationFindAllRequest request = new ConversationFindAllRequest(
            null,
            null,
            null,
            20,
            "INVALID",
            "createdAt"
        );

        assertThatThrownBy(() -> conversationService.findAll(requestUserId, request))
            .isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_SORT_DIRECTION)
            );
    }

    @Test
    @DisplayName("특정 사용자와의 대화방이 없으면 대화방 없음 예외를 던진다")
    void findWithUser_notFound() {
        UUID requestUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID withUserId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        given(conversationRepository.findByUserIds(requestUserId, withUserId))
            .willReturn(Optional.empty());

        assertThatThrownBy(() -> conversationService.findWithUser(requestUserId, withUserId))
            .isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONVERSATION_NOT_FOUND)
            );
    }

    @Test
    @DisplayName("대화방 참여자가 아닌 사용자가 ID로 조회하면 대화방 없음 예외를 던진다")
    void find_notParticipant() {
        UUID requestUserId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        UUID conversationId = UUID.fromString("00000000-0000-0000-0000-000000000011");
        Conversation conversation = conversation(
            conversationId,
            user(UUID.fromString("00000000-0000-0000-0000-000000000001"), "a@test.com", "A"),
            user(UUID.fromString("00000000-0000-0000-0000-000000000002"), "b@test.com", "B"),
            Instant.parse("2026-06-29T00:00:00Z")
        );

        given(conversationRepository.findWithUsersById(conversationId))
            .willReturn(Optional.of(conversation));

        assertThatThrownBy(() -> conversationService.find(requestUserId, conversationId))
            .isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONVERSATION_NOT_FOUND)
            );
    }

    private User user(UUID id, String email, String name) {
        User user = new User(email, name, "password", null, UserRole.USER);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Conversation conversation(UUID id, User user1, User user2, Instant createdAt) {
        Conversation conversation = new Conversation(user1, user2);
        ReflectionTestUtils.setField(conversation, "id", id);
        ReflectionTestUtils.setField(conversation, "createdAt", createdAt);
        return conversation;
    }
}
