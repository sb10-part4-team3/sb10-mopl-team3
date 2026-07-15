package com.example.sb10_MoPl_team3.notification.repository;

import com.example.sb10_MoPl_team3.notification.entity.NotificationFanoutDlq;
import com.example.sb10_MoPl_team3.notification.enums.NotificationFanoutDlqStatus;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationFanoutDlqRepository extends JpaRepository<NotificationFanoutDlq, UUID> {

    Slice<NotificationFanoutDlq> findByStatusOrderByCreatedAtDescIdDesc(
            NotificationFanoutDlqStatus status,
            Pageable pageable
    );
}
