package com.example.sb10_MoPl_team3.notification.service;

import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.notification.config.NotificationKafkaTopics;
import com.example.sb10_MoPl_team3.notification.dto.NotificationFanoutDlqDto;
import com.example.sb10_MoPl_team3.notification.entity.NotificationFanoutDlq;
import com.example.sb10_MoPl_team3.notification.enums.NotificationFanoutDlqStatus;
import com.example.sb10_MoPl_team3.notification.kafka.NotificationFanoutKafkaMessage;
import com.example.sb10_MoPl_team3.notification.repository.NotificationFanoutDlqRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationFanoutDlqService {

    private final NotificationFanoutDlqRepository repository;
    private final KafkaTemplate<String, NotificationFanoutKafkaMessage> kafkaTemplate;
    private final Clock clock;

    @Transactional
    public NotificationFanoutDlqDto save(NotificationFanoutKafkaMessage message, String errorMessage) {
        return NotificationFanoutDlqDto.from(repository.save(
                new NotificationFanoutDlq(message, errorMessage)));
    }

    @Transactional(readOnly = true)
    public List<NotificationFanoutDlqDto> findPending(int limit) {
        int normalizedLimit = Math.min(Math.max(limit, 1), 100);
        return repository.findByStatusOrderByCreatedAtDescIdDesc(
                        NotificationFanoutDlqStatus.PENDING,
                        PageRequest.of(0, normalizedLimit))
                .stream()
                .map(NotificationFanoutDlqDto::from)
                .toList();
    }

    @Transactional
    public NotificationFanoutDlqDto retry(UUID dlqId) {
        NotificationFanoutDlq dlq = repository.findById(dlqId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (dlq.getStatus() == NotificationFanoutDlqStatus.RETRIED) {
            return NotificationFanoutDlqDto.from(dlq);
        }

        NotificationFanoutKafkaMessage message = dlq.toMessage();
        kafkaTemplate.send(NotificationKafkaTopics.FANOUT, message.outboxId().toString(), message)
                .join();
        dlq.markRetried(clock.instant());
        return NotificationFanoutDlqDto.from(dlq);
    }
}
