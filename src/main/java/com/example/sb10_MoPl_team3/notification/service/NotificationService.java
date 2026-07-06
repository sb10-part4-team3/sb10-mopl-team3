package com.example.sb10_MoPl_team3.notification.service;

import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.global.sse.SseEventPublisher;
import com.example.sb10_MoPl_team3.notification.dto.NotificationDto;
import com.example.sb10_MoPl_team3.notification.entity.Notification;
import com.example.sb10_MoPl_team3.notification.event.NotificationEvent;
import com.example.sb10_MoPl_team3.notification.event.NotificationEventHandler;
import com.example.sb10_MoPl_team3.notification.repository.NotificationRepository;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService implements NotificationEventHandler {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SseEventPublisher sseEventPublisher;

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
    }
}
