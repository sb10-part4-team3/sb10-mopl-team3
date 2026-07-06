package com.example.sb10_MoPl_team3.notification.event;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.sb10_MoPl_team3.notification.enums.NotificationLevel;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificationEventTest {

    @Test
    @DisplayName("알림 이벤트의 필수 값이 없으면 생성할 수 없다")
    void create_rejectsMissingRequiredValues() {
        UUID receiverId = UUID.randomUUID();

        assertThatThrownBy(() -> new NotificationEvent(
                null, "제목", "내용", NotificationLevel.INFO))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new NotificationEvent(
                receiverId, " ", "내용", NotificationLevel.INFO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NotificationEvent(
                receiverId, "제목", " ", NotificationLevel.INFO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NotificationEvent(
                receiverId, "제목", "내용", null))
                .isInstanceOf(NullPointerException.class);
    }
}
