package com.example.sb10_MoPl_team3.notification.service;

import com.example.sb10_MoPl_team3.notification.entity.NotificationFanoutOutbox;
import com.example.sb10_MoPl_team3.notification.event.NotificationFanoutEvent;
import com.example.sb10_MoPl_team3.notification.repository.NotificationFanoutOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationFanoutOutboxService {

    private final NotificationFanoutOutboxRepository repository;

    @Transactional
    public NotificationFanoutOutbox save(NotificationFanoutEvent event) {
        return repository.save(new NotificationFanoutOutbox(event));
    }
}
