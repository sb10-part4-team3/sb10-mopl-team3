package com.example.sb10_MoPl_team3.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sb10_MoPl_team3.global.config.JpaAuditingConfig;
import com.example.sb10_MoPl_team3.global.config.QuerydslConfig;
import com.example.sb10_MoPl_team3.notification.entity.Notification;
import com.example.sb10_MoPl_team3.notification.enums.NotificationLevel;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import({QuerydslConfig.class, JpaAuditingConfig.class})
class NotificationRepositoryCustomImplTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("수신자 본인의 미읽음 알림만 createdAt 오름차순으로 조회한다")
    void findByReceiverIdAsc_onlyReceiverUnread() {
        User receiver = saveUser("receiver@test.com", "수신자");
        User other = saveUser("other@test.com", "다른사용자");
        Notification older = saveNotification(
                receiver,
                "오래된 알림",
                Instant.parse("2026-06-29T00:00:00Z"));
        Notification newer = saveNotification(
                receiver,
                "최근 알림",
                Instant.parse("2026-06-29T00:01:00Z"));
        saveNotification(other, "다른 사용자 알림", Instant.parse("2026-06-29T00:02:00Z"));
        Notification read = saveNotification(
                receiver,
                "읽은 알림",
                Instant.parse("2026-06-29T00:03:00Z"));
        read.markAsRead();
        notificationRepository.saveAndFlush(read);

        em.clear();

        List<Notification> result = notificationRepository.findByReceiverIdAsc(
                receiver.getId(),
                null,
                null,
                PageRequest.of(0, 10));
        long count = notificationRepository.countUnreadByReceiverId(receiver.getId());

        assertThat(result)
                .extracting(Notification::getId)
                .containsExactly(older.getId(), newer.getId());
        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("내림차순 커서 이후 알림만 조회한다")
    void findByReceiverIdDesc_cursor() {
        User receiver = saveUser("receiver@test.com", "수신자");
        Notification oldest = saveNotification(
                receiver,
                "오래된 알림",
                Instant.parse("2026-06-29T00:00:00Z"));
        Notification cursor = saveNotification(
                receiver,
                "커서 알림",
                Instant.parse("2026-06-29T00:01:00Z"));
        saveNotification(receiver, "최근 알림", Instant.parse("2026-06-29T00:02:00Z"));

        em.clear();

        List<Notification> result = notificationRepository.findByReceiverIdDesc(
                receiver.getId(),
                cursor.getCreatedAt(),
                cursor.getId(),
                PageRequest.of(0, 10));

        assertThat(result)
                .extracting(Notification::getId)
                .containsExactly(oldest.getId());
    }

    private User saveUser(String email, String name) {
        User user = new User(email, name, "password", null, UserRole.USER);
        return userRepository.saveAndFlush(user);
    }

    private Notification saveNotification(User receiver, String title, Instant createdAt) {
        Notification notification = notificationRepository.saveAndFlush(
                new Notification(receiver, title, "내용", NotificationLevel.INFO));
        updateCreatedAt(notification.getId(), createdAt);
        ReflectionTestUtils.setField(notification, "createdAt", createdAt);
        em.flush();
        return notification;
    }

    private void updateCreatedAt(UUID id, Instant createdAt) {
        em.createQuery("update Notification n set n.createdAt = :createdAt where n.id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", id)
                .executeUpdate();
    }
}
