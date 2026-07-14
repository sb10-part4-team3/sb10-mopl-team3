package com.example.sb10_MoPl_team3.notification.repository;

import com.example.sb10_MoPl_team3.notification.entity.NotificationFanoutOutbox;
import com.example.sb10_MoPl_team3.notification.enums.NotificationFanoutOutboxStatus;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationFanoutOutboxRepository
        extends JpaRepository<NotificationFanoutOutbox, UUID> {

    Slice<NotificationFanoutOutbox> findByStatusOrderByCreatedAtAscIdAsc(
            NotificationFanoutOutboxStatus status,
            Pageable pageable
    );

    Slice<NotificationFanoutOutbox> findByStatusInOrderByCreatedAtAscIdAsc(
            Collection<NotificationFanoutOutboxStatus> statuses,
            Pageable pageable
    );
}
