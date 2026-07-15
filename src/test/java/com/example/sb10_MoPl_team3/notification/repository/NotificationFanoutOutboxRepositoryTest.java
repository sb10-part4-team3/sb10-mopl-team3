package com.example.sb10_MoPl_team3.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sb10_MoPl_team3.global.config.JpaAuditingConfig;
import com.example.sb10_MoPl_team3.global.config.QuerydslConfig;
import com.example.sb10_MoPl_team3.notification.entity.NotificationFanoutOutbox;
import com.example.sb10_MoPl_team3.notification.enums.NotificationFanoutOutboxStatus;
import com.example.sb10_MoPl_team3.notification.enums.NotificationLevel;
import com.example.sb10_MoPl_team3.notification.event.NotificationAudienceType;
import com.example.sb10_MoPl_team3.notification.event.NotificationFanoutEvent;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
class NotificationFanoutOutboxRepositoryTest {

    @Autowired
    NotificationFanoutOutboxRepository repository;

    @Test
    @DisplayName("팬아웃 이벤트를 outbox에 저장하고 다시 이벤트로 복원한다")
    void saveAndRestoreEvent() {
        NotificationFanoutEvent event = event("시청 시작");

        NotificationFanoutOutbox saved = repository.saveAndFlush(
                new NotificationFanoutOutbox(event));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(NotificationFanoutOutboxStatus.PENDING);
        assertThat(saved.getRetryCount()).isZero();
        assertThat(saved.toEvent()).isEqualTo(event);
    }

    @Test
    @DisplayName("상태별 outbox를 생성 순서로 조회한다")
    void findByStatusOrderByCreatedAtAscIdAsc() {
        NotificationFanoutOutbox first = repository.saveAndFlush(
                new NotificationFanoutOutbox(event("첫 번째")));
        NotificationFanoutOutbox second = repository.saveAndFlush(
                new NotificationFanoutOutbox(event("두 번째")));
        second.markProcessing();
        repository.saveAndFlush(second);

        var result = repository.findByStatusOrderByCreatedAtAscIdAsc(
                NotificationFanoutOutboxStatus.PENDING,
                PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(NotificationFanoutOutbox::getId)
                .containsExactly(first.getId());
    }

    @Test
    @DisplayName("발행 성공과 실패 상태를 기록한다")
    void updatePublishStatus() {
        NotificationFanoutOutbox outbox = repository.saveAndFlush(
                new NotificationFanoutOutbox(event("알림")));

        outbox.markPublishFailed("kafka publish failed");
        repository.saveAndFlush(outbox);

        NotificationFanoutOutbox failed = repository.findById(outbox.getId()).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(NotificationFanoutOutboxStatus.PUBLISH_FAILED);
        assertThat(failed.getRetryCount()).isEqualTo(1);
        assertThat(failed.getLastError()).isEqualTo("kafka publish failed");

        Instant publishedAt = Instant.parse("2026-07-09T00:00:00Z");
        failed.markPublished(publishedAt);
        repository.saveAndFlush(failed);

        NotificationFanoutOutbox published = repository.findById(outbox.getId()).orElseThrow();
        assertThat(published.getStatus()).isEqualTo(NotificationFanoutOutboxStatus.PUBLISHED);
        assertThat(published.getPublishedAt()).isEqualTo(publishedAt);
        assertThat(published.getLastError()).isNull();
    }

    private NotificationFanoutEvent event(String title) {
        return new NotificationFanoutEvent(
                NotificationAudienceType.FOLLOWERS,
                UUID.randomUUID(),
                title,
                "새로운 활동입니다.",
                NotificationLevel.INFO
        );
    }
}
