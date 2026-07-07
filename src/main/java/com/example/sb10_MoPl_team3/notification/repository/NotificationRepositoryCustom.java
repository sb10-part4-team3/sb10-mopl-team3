package com.example.sb10_MoPl_team3.notification.repository;

import com.example.sb10_MoPl_team3.notification.entity.Notification;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface NotificationRepositoryCustom {

    List<Notification> findByReceiverIdAsc(
            UUID receiverId,
            Instant cursor,
            UUID idAfter,
            Pageable pageable
    );

    List<Notification> findByReceiverIdDesc(
            UUID receiverId,
            Instant cursor,
            UUID idAfter,
            Pageable pageable
    );

    long countByReceiverId(UUID receiverId);
}
