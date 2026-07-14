package com.example.sb10_MoPl_team3.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.global.sse.SseConnectionRepository;
import com.example.sb10_MoPl_team3.global.sse.SseEventPublisher;
import com.example.sb10_MoPl_team3.notification.dto.NotificationFindAllRequest;
import com.example.sb10_MoPl_team3.notification.dto.NotificationDto;
import com.example.sb10_MoPl_team3.notification.entity.Notification;
import com.example.sb10_MoPl_team3.notification.enums.NotificationLevel;
import com.example.sb10_MoPl_team3.notification.event.NotificationEvent;
import com.example.sb10_MoPl_team3.notification.repository.NotificationRepository;
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

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SseEventPublisher sseEventPublisher;

    @Mock
    private SseConnectionRepository sseConnectionRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("본인의 알림 목록을 커서 응답으로 반환한다")
    void findAll_success() {
        UUID receiverId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        User receiver = user();
        ReflectionTestUtils.setField(receiver, "id", receiverId);
        Notification first = notification(
                UUID.fromString("00000000-0000-0000-0000-000000000011"),
                receiver,
                "첫 알림",
                Instant.parse("2026-06-29T00:00:00Z"));
        Notification second = notification(
                UUID.fromString("00000000-0000-0000-0000-000000000012"),
                receiver,
                "두 번째 알림",
                Instant.parse("2026-06-29T00:01:00Z"));
        Notification extra = notification(
                UUID.fromString("00000000-0000-0000-0000-000000000013"),
                receiver,
                "추가 알림",
                Instant.parse("2026-06-29T00:02:00Z"));
        NotificationFindAllRequest request = new NotificationFindAllRequest(
                "2026-06-29T00:00:00Z",
                first.getId(),
                2,
                "ASCENDING",
                "createdAt");

        given(notificationRepository.findByReceiverIdAsc(
                org.mockito.ArgumentMatchers.eq(receiverId),
                org.mockito.ArgumentMatchers.eq(Instant.parse("2026-06-29T00:00:00Z")),
                org.mockito.ArgumentMatchers.eq(first.getId()),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        )).willReturn(List.of(first, second, extra));
        given(notificationRepository.countUnreadByReceiverId(receiverId)).willReturn(3L);

        var response = notificationService.findAll(receiverId, request);

        assertThat(response.data()).hasSize(2);
        assertThat(response.data().get(0).title()).isEqualTo("첫 알림");
        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextCursor()).isEqualTo("2026-06-29T00:01:00Z");
        assertThat(response.nextIdAfter()).isEqualTo(second.getId());
        assertThat(response.totalCount()).isEqualTo(3L);
        assertThat(response.sortBy()).isEqualTo("createdAt");
        assertThat(response.sortDirection()).isEqualTo("ASCENDING");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        then(notificationRepository).should().findByReceiverIdAsc(
                org.mockito.ArgumentMatchers.eq(receiverId),
                org.mockito.ArgumentMatchers.eq(Instant.parse("2026-06-29T00:00:00Z")),
                org.mockito.ArgumentMatchers.eq(first.getId()),
                pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(3);
        then(notificationRepository).should(never()).findByReceiverIdDesc(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    @DisplayName("알림 목록 조회에서 커서와 보조 커서가 함께 전달되지 않으면 커서 예외를 던진다")
    void findAll_invalidCursorPair() {
        UUID receiverId = UUID.randomUUID();
        NotificationFindAllRequest request = new NotificationFindAllRequest(
                "2026-06-29T00:00:00Z",
                null,
                20,
                "DESCENDING",
                "createdAt");

        assertThatThrownBy(() -> notificationService.findAll(receiverId, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_CURSOR));
    }

    @Test
    @DisplayName("알림 목록 조회 정렬 방향이 올바르지 않으면 정렬 예외를 던진다")
    void findAll_invalidSortDirection() {
        UUID receiverId = UUID.randomUUID();
        NotificationFindAllRequest request = new NotificationFindAllRequest(
                null,
                null,
                20,
                "INVALID",
                "createdAt");

        assertThatThrownBy(() -> notificationService.findAll(receiverId, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_SORT_DIRECTION));
    }

    @Test
    @DisplayName("알림 목록 조회 정렬 기준이 없거나 올바르지 않으면 입력값 예외를 던진다")
    void findAll_invalidSortBy() {
        UUID receiverId = UUID.randomUUID();
        NotificationFindAllRequest request = new NotificationFindAllRequest(
                null,
                null,
                20,
                "DESCENDING",
                "");

        assertThatThrownBy(() -> notificationService.findAll(receiverId, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    @DisplayName("알림 목록 조회 limit이 0 이하이면 입력값 예외를 던진다")
    void findAll_invalidLimit() {
        UUID receiverId = UUID.randomUUID();
        NotificationFindAllRequest request = new NotificationFindAllRequest(
                null,
                null,
                0,
                "DESCENDING",
                "createdAt");

        assertThatThrownBy(() -> notificationService.findAll(receiverId, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    @DisplayName("알림 목록 조회 커서 형식이 올바르지 않으면 원인 예외를 보존한다")
    void findAll_invalidCursorFormat_preservesCause() {
        UUID receiverId = UUID.randomUUID();
        NotificationFindAllRequest request = new NotificationFindAllRequest(
                "invalid-cursor",
                UUID.randomUUID(),
                20,
                "DESCENDING",
                "createdAt");

        assertThatThrownBy(() -> notificationService.findAll(receiverId, request))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_CURSOR);
                    assertThat(exception.getCause()).isInstanceOf(RuntimeException.class);
                });
    }

    @Test
    @DisplayName("알림 이벤트를 수신하면 알림 엔티티를 저장한다")
    void handle_persistsNotification() {
        UUID receiverId = UUID.randomUUID();
        User receiver = user();
        ReflectionTestUtils.setField(receiver, "id", receiverId);
        NotificationEvent event = new NotificationEvent(
                receiverId, "제목", "내용", NotificationLevel.INFO);
        given(userRepository.findById(receiverId)).willReturn(Optional.of(receiver));
        given(notificationRepository.save(org.mockito.ArgumentMatchers.any(Notification.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        notificationService.handle(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        then(notificationRepository).should().save(captor.capture());
        assertThat(captor.getValue().getReceiver()).isEqualTo(receiver);
        assertThat(captor.getValue().getTitle()).isEqualTo("제목");
        assertThat(captor.getValue().getContent()).isEqualTo("내용");
        assertThat(captor.getValue().getLevel()).isEqualTo(NotificationLevel.INFO);
        ArgumentCaptor<NotificationDto> dtoCaptor = ArgumentCaptor.forClass(NotificationDto.class);
        then(sseEventPublisher).should().publishAfterCommit(
                org.mockito.ArgumentMatchers.eq(receiverId),
                org.mockito.ArgumentMatchers.eq(SseEventPublisher.NOTIFICATIONS_EVENT),
                dtoCaptor.capture());
        assertThat(dtoCaptor.getValue().receiverId()).isEqualTo(receiverId);
        assertThat(dtoCaptor.getValue().title()).isEqualTo("제목");
        assertThat(dtoCaptor.getValue().content()).isEqualTo("내용");
        assertThat(dtoCaptor.getValue().level()).isEqualTo(NotificationLevel.INFO);
    }

    @Test
    @DisplayName("본인의 알림을 읽으면 읽음 상태로 변경하고 SSE 알림 캐시를 삭제한다")
    void read_marksNotificationAsRead() {
        UUID receiverId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        Notification notification = new Notification(
                user(), "제목", "내용", NotificationLevel.INFO);
        given(notificationRepository.findByIdAndReceiverId(notificationId, receiverId))
                .willReturn(Optional.of(notification));

        notificationService.read(receiverId, notificationId);

        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();
        then(sseConnectionRepository).should().deleteCachedEventByDataId(
                receiverId,
                SseEventPublisher.NOTIFICATIONS_EVENT,
                notificationId);
    }

    @Test
    @DisplayName("알림 읽음 처리 중 SSE 캐시 삭제 실패는 읽음 상태 변경을 막지 않는다")
    void read_ignoresSseCacheDeleteFailure() {
        UUID receiverId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        Notification notification = new Notification(
                user(), "제목", "내용", NotificationLevel.INFO);
        given(notificationRepository.findByIdAndReceiverId(notificationId, receiverId))
                .willReturn(Optional.of(notification));
        willThrow(new RuntimeException("redis unavailable"))
                .given(sseConnectionRepository)
                .deleteCachedEventByDataId(
                        receiverId,
                        SseEventPublisher.NOTIFICATIONS_EVENT,
                        notificationId);

        notificationService.read(receiverId, notificationId);

        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();
    }

    @Test
    @DisplayName("본인 소유가 아닌 알림은 읽음 처리할 수 없다")
    void read_rejectsUnknownOrOtherUsersNotification() {
        UUID receiverId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        given(notificationRepository.findByIdAndReceiverId(notificationId, receiverId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.read(receiverId, notificationId))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND));
        then(notificationRepository).should().findByIdAndReceiverId(notificationId, receiverId);
        then(notificationRepository).shouldHaveNoMoreInteractions();
    }

    private User user() {
        return new User("user@test.com", "사용자", "password", null, UserRole.USER);
    }

    private Notification notification(UUID id, User receiver, String title, Instant createdAt) {
        Notification notification = new Notification(receiver, title, "내용", NotificationLevel.INFO);
        ReflectionTestUtils.setField(notification, "id", id);
        ReflectionTestUtils.setField(notification, "createdAt", createdAt);
        return notification;
    }
}
