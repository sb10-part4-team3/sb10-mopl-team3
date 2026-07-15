package com.example.sb10_MoPl_team3.notification.repository;

import com.example.sb10_MoPl_team3.notification.entity.NotificationFanoutOutbox;
import com.example.sb10_MoPl_team3.notification.enums.NotificationFanoutOutboxStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationFanoutOutboxRepository
        extends JpaRepository<NotificationFanoutOutbox, UUID> {

    Slice<NotificationFanoutOutbox> findByStatusOrderByCreatedAtAscIdAsc(
            NotificationFanoutOutboxStatus status,
            Pageable pageable
    );

    @Query(
            value = """
                    select *
                    from notification_fanout_outbox
                    where status in (:statuses)
                    order by created_at asc, id asc
                    limit :limit
                    for update skip locked
                    """,
            nativeQuery = true
    )
    List<NotificationFanoutOutbox> findClaimTargets(
            @Param("statuses") Collection<String> statuses,
            @Param("limit") int limit
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update NotificationFanoutOutbox outbox
            set outbox.status = :pendingStatus
            where outbox.status = :processingStatus
              and outbox.updatedAt < :staleBefore
            """)
    int resetStaleProcessing(
            @Param("processingStatus") NotificationFanoutOutboxStatus processingStatus,
            @Param("pendingStatus") NotificationFanoutOutboxStatus pendingStatus,
            @Param("staleBefore") Instant staleBefore
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update NotificationFanoutOutbox outbox
            set outbox.status = :status,
                outbox.publishedAt = :publishedAt,
                outbox.lastError = null
            where outbox.id = :outboxId
            """)
    int updatePublished(
            @Param("outboxId") UUID outboxId,
            @Param("status") NotificationFanoutOutboxStatus status,
            @Param("publishedAt") Instant publishedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update NotificationFanoutOutbox outbox
            set outbox.status = :status,
                outbox.retryCount = outbox.retryCount + 1,
                outbox.lastError = :errorMessage
            where outbox.id = :outboxId
            """)
    int updatePublishFailed(
            @Param("outboxId") UUID outboxId,
            @Param("status") NotificationFanoutOutboxStatus status,
            @Param("errorMessage") String errorMessage
    );
}
