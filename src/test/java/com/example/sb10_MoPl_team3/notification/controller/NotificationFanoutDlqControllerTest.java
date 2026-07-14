package com.example.sb10_MoPl_team3.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.sb10_MoPl_team3.notification.dto.NotificationFanoutDlqDto;
import com.example.sb10_MoPl_team3.notification.enums.NotificationFanoutDlqStatus;
import com.example.sb10_MoPl_team3.notification.enums.NotificationLevel;
import com.example.sb10_MoPl_team3.notification.event.NotificationAudienceType;
import com.example.sb10_MoPl_team3.notification.service.NotificationFanoutDlqService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

@ExtendWith(MockitoExtension.class)
class NotificationFanoutDlqControllerTest {

    @Mock
    NotificationFanoutDlqService dlqService;

    @Test
    @DisplayName("PENDING DLQ 목록을 조회한다")
    void findPending() {
        NotificationFanoutDlqDto dto = dto(NotificationFanoutDlqStatus.PENDING);
        given(dlqService.findPending(10)).willReturn(List.of(dto));
        NotificationFanoutDlqController controller = new NotificationFanoutDlqController(dlqService);

        var result = controller.findPending(10);

        assertThat(result).containsExactly(dto);
        then(dlqService).should().findPending(10);
    }

    @Test
    @DisplayName("DLQ 재처리를 요청한다")
    void retry() {
        UUID dlqId = UUID.randomUUID();
        NotificationFanoutDlqDto dto = dto(NotificationFanoutDlqStatus.RETRIED);
        given(dlqService.retry(dlqId)).willReturn(dto);
        NotificationFanoutDlqController controller = new NotificationFanoutDlqController(dlqService);

        var result = controller.retry(dlqId);

        assertThat(result).isEqualTo(dto);
        then(dlqService).should().retry(dlqId);
    }

    @Test
    @DisplayName("DLQ 운영 API는 관리자 권한을 요구한다")
    void endpoints_requireAdminRole() throws NoSuchMethodException {
        PreAuthorize findPending = NotificationFanoutDlqController.class
                .getMethod("findPending", int.class)
                .getAnnotation(PreAuthorize.class);
        PreAuthorize retry = NotificationFanoutDlqController.class
                .getMethod("retry", UUID.class)
                .getAnnotation(PreAuthorize.class);

        assertThat(findPending.value()).isEqualTo("hasRole('ADMIN')");
        assertThat(retry.value()).isEqualTo("hasRole('ADMIN')");
    }

    private NotificationFanoutDlqDto dto(NotificationFanoutDlqStatus status) {
        return new NotificationFanoutDlqDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                NotificationAudienceType.FOLLOWERS,
                UUID.randomUUID(),
                "시청 시작",
                "새로운 활동입니다.",
                NotificationLevel.INFO,
                "retry exhausted",
                status,
                Instant.parse("2026-07-14T00:00:00Z"),
                status == NotificationFanoutDlqStatus.RETRIED
                        ? Instant.parse("2026-07-14T00:01:00Z")
                        : null
        );
    }
}
