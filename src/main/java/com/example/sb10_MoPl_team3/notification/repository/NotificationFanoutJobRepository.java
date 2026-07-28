package com.example.sb10_MoPl_team3.notification.repository;

import com.example.sb10_MoPl_team3.notification.entity.NotificationFanoutJob;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationFanoutJobRepository extends JpaRepository<NotificationFanoutJob, UUID> {

    Optional<NotificationFanoutJob> findByOutboxId(UUID outboxId);
}
