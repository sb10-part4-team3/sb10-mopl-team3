package com.example.sb10_MoPl_team3.notification.repository;

import com.example.sb10_MoPl_team3.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
}
