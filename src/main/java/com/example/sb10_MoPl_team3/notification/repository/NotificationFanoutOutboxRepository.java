package com.example.sb10_MoPl_team3.notification.repository;

import com.example.sb10_MoPl_team3.notification.entity.NotificationFanoutOutbox;
import com.example.sb10_MoPl_team3.notification.enums.NotificationFanoutOutboxStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificationFanoutOutboxRepository
        extends JpaRepository<NotificationFanoutOutbox, UUID> {

    Slice<NotificationFanoutOutbox> findByStatusOrderByCreatedAtAscIdAsc(
            NotificationFanoutOutboxStatus status,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Slice<NotificationFanoutOutbox> findByStatusInOrderByCreatedAtAscIdAsc(
            Collection<NotificationFanoutOutboxStatus> statuses,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update NotificationFanoutOutbox outbox
            set outbox.status = :pendingStatus
            where outbox.status = :processingStatus
              and outbox.updatedAt < :staleBefore
            """)
    int resetStaleProcessing(
            NotificationFanoutOutboxStatus processingStatus,
            NotificationFanoutOutboxStatus pendingStatus,
            Instant staleBefore
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update NotificationFanoutOutbox outbox
            set outbox.status = :status,
                outbox.publishedAt = :publishedAt,
                outbox.lastError = null
            where outbox.id = :outboxId
            """)
    int updatePublished(UUID outboxId, NotificationFanoutOutboxStatus status, Instant publishedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update NotificationFanoutOutbox outbox
            set outbox.status = :status,
                outbox.retryCount = outbox.retryCount + 1,
                outbox.lastError = :errorMessage
            where outbox.id = :outboxId
            """)
    int updatePublishFailed(UUID outboxId, NotificationFanoutOutboxStatus status, String errorMessage);
}
