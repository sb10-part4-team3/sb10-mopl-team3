package com.example.sb10_MoPl_team3.notification.service;

import com.example.sb10_MoPl_team3.global.sse.SseEventPublisher;
import com.example.sb10_MoPl_team3.notification.dto.NotificationDto;
import com.example.sb10_MoPl_team3.notification.entity.Notification;
import com.example.sb10_MoPl_team3.notification.event.NotificationFanoutEvent;
import com.example.sb10_MoPl_team3.notification.repository.NotificationRepository;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationFanoutBatchService {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final SseEventPublisher sseEventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int saveBatch(List<UUID> receiverIds, NotificationFanoutEvent event) {
        if (receiverIds.isEmpty()) {
            return 0;
        }

        List<User> receivers = userRepository.findAllById(receiverIds);
        List<Notification> notifications = receivers.stream()
                .map(receiver -> new Notification(
                        receiver, event.title(), event.content(), event.level()))
                .toList();
        List<Notification> savedNotifications = notificationRepository.saveAll(notifications);
        savedNotifications.forEach(notification -> sseEventPublisher.publishAfterCommit(
                notification.getReceiver().getId(),
                SseEventPublisher.NOTIFICATIONS_EVENT,
                NotificationDto.from(notification)));
        return savedNotifications.size();
    }
}
