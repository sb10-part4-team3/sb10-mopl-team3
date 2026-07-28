package com.example.sb10_MoPl_team3.notification.repository;

import com.example.sb10_MoPl_team3.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, UUID>, NotificationRepositoryCustom {

    Optional<Notification> findByIdAndReceiverId(UUID id, UUID receiverId);

    @Query("""
            select notification.receiver.id
              from Notification notification
             where notification.fanoutOutboxId = :fanoutOutboxId
               and notification.receiver.id in :receiverIds
            """)
    Set<UUID> findReceiverIdsByFanoutOutboxIdAndReceiverIdIn(
            UUID fanoutOutboxId,
            Collection<UUID> receiverIds
    );
}
