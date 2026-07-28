package com.example.sb10_MoPl_team3.notification.service;

import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.global.sse.SseConnectionRepository;
import com.example.sb10_MoPl_team3.global.sse.SseEventPublisher;
import com.example.sb10_MoPl_team3.notification.dto.CursorResponseNotificationDto;
import com.example.sb10_MoPl_team3.notification.dto.NotificationFindAllRequest;
import com.example.sb10_MoPl_team3.notification.dto.NotificationDto;
import com.example.sb10_MoPl_team3.notification.entity.Notification;
import com.example.sb10_MoPl_team3.notification.event.NotificationEvent;
import com.example.sb10_MoPl_team3.notification.event.NotificationEventHandler;
import com.example.sb10_MoPl_team3.notification.repository.NotificationRepository;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService implements NotificationEventHandler {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SseEventPublisher sseEventPublisher;
    private final SseConnectionRepository sseConnectionRepository;

    @Transactional(readOnly = true)
    public CursorResponseNotificationDto<NotificationDto> findAll(
            UUID receiverId,
            NotificationFindAllRequest request
    ) {
        String sortBy = normalizeSortBy(request.sortBy());
        String sortDirection = normalizeSortDirection(request.sortDirection());
        boolean ascending = sortDirection.equals("ASCENDING");
        int limit = normalizeLimit(request.limit());
        Instant cursor = parseCursor(request.cursor(), request.idAfter());

        List<Notification> fetchedNotifications = ascending
                ? notificationRepository.findByReceiverIdAsc(
                        receiverId,
                        cursor,
                        request.idAfter(),
                        PageRequest.of(0, limit + 1))
                : notificationRepository.findByReceiverIdDesc(
                        receiverId,
                        cursor,
                        request.idAfter(),
                        PageRequest.of(0, limit + 1));

        boolean hasNext = fetchedNotifications.size() > limit;
        List<Notification> notifications = hasNext
                ? fetchedNotifications.subList(0, limit)
                : fetchedNotifications;

        String nextCursor = null;
        UUID nextIdAfter = null;
        if (hasNext && !notifications.isEmpty()) {
            Notification lastNotification = notifications.get(notifications.size() - 1);
            nextCursor = lastNotification.getCreatedAt().toString();
            nextIdAfter = lastNotification.getId();
        }

        List<NotificationDto> data = notifications.stream()
                .map(NotificationDto::from)
                .toList();
        long totalCount = notificationRepository.countUnreadByReceiverId(receiverId);

        return new CursorResponseNotificationDto<>(
                data,
                nextCursor,
                nextIdAfter,
                hasNext,
                totalCount,
                sortBy,
                sortDirection);
    }

    @Override
    @Transactional
    public void handle(NotificationEvent event) {
        User receiver = userRepository.findById(event.receiverId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Notification notification = notificationRepository.save(new Notification(
                receiver,
                event.title(),
                event.content(),
                event.level()
        ));
        sseEventPublisher.publishAfterCommit(
                event.receiverId(),
                SseEventPublisher.NOTIFICATIONS_EVENT,
                NotificationDto.from(notification));
    }

    @Transactional
    public void read(UUID receiverId, UUID notificationId) {
        Notification notification = notificationRepository
                .findByIdAndReceiverId(notificationId, receiverId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
        notification.markAsRead();
        try {
            sseConnectionRepository.deleteCachedEventByDataId(
                    receiverId,
                    SseEventPublisher.NOTIFICATIONS_EVENT,
                    notificationId);
        } catch (RuntimeException exception) {
            log.warn("알림 SSE 캐시 삭제 실패: receiverId={}, notificationId={}",
                    receiverId,
                    notificationId,
                    exception);
        }
    }

    private String normalizeSortBy(String sortBy) {
        if ("createdAt".equalsIgnoreCase(sortBy)) {
            return "createdAt";
        }

        throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }

    private String normalizeSortDirection(String sortDirection) {
        if (sortDirection == null || sortDirection.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_SORT_DIRECTION);
        }

        String normalized = sortDirection.toUpperCase(Locale.ROOT);
        if (!normalized.equals("ASCENDING") && !normalized.equals("DESCENDING")) {
            throw new BusinessException(ErrorCode.INVALID_SORT_DIRECTION);
        }

        return normalized;
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return Math.min(limit, 100);
    }

    private Instant parseCursor(String cursor, UUID idAfter) {
        boolean hasCursor = cursor != null && !cursor.isBlank();
        boolean hasIdAfter = idAfter != null;

        if (hasCursor != hasIdAfter) {
            throw new BusinessException(ErrorCode.INVALID_CURSOR);
        }

        if (!hasCursor) {
            return null;
        }

        try {
            return Instant.parse(cursor);
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.INVALID_CURSOR, exception);
        }
    }
}
