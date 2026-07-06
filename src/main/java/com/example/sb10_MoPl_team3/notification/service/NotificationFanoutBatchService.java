package com.example.sb10_MoPl_team3.notification.service;

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
        notificationRepository.saveAll(notifications);
        return notifications.size();
    }
}
