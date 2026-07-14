package com.example.sb10_MoPl_team3.sportsdb.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.example.sb10_MoPl_team3.global.exception.SportsDbApiException;
import com.example.sb10_MoPl_team3.notification.enums.NotificationLevel;
import com.example.sb10_MoPl_team3.notification.event.NotificationEvent;
import com.example.sb10_MoPl_team3.sportsdb.client.SportsDbApiClient;
import com.example.sb10_MoPl_team3.sportsdb.config.SportsDbProperties;
import com.example.sb10_MoPl_team3.sportsdb.dto.SportsDbEventsResponse;
import com.example.sb10_MoPl_team3.sportsdb.dto.SportsDbEventsResponse.SportsDbEvent;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SportsDbContentSyncServiceTest {

    private static final String LEAGUE_1 = "4328";
    private static final String LEAGUE_2 = "4329";

    @Mock
    private SportsDbApiClient sportsDbApiClient;

    @Mock
    private SportsDbProperties sportsDbProperties;

    @Mock
    private SportsDbContentPersister sportsDbContentPersister;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private UserRepository userRepository;

    private SportsDbContentSyncService sportsDbContentSyncService;

    @BeforeEach
    void setUp() {
        sportsDbContentSyncService = new SportsDbContentSyncService(
            sportsDbApiClient, sportsDbProperties, sportsDbContentPersister,
            eventPublisher, userRepository);
    }

    @Test
    @DisplayName("첫 시도에 성공하면 재시도 없이 바로 저장하고 알림도 발행하지 않는다")
    void syncNextEvents_첫_시도_성공() {
        given(sportsDbProperties.getTargetLeagueIds()).willReturn(List.of(LEAGUE_1));
        given(sportsDbApiClient.getNextEventsByLeague(LEAGUE_1)).willReturn(sampleResponse());

        sportsDbContentSyncService.syncNextEvents();

        then(sportsDbApiClient).should(times(1)).getNextEventsByLeague(LEAGUE_1);
        then(sportsDbContentPersister).should().persistEvents(sampleResponse().events());
        then(eventPublisher).should(never()).publishEvent(any());
    }

    @Test
    @DisplayName("몇 번 실패하다가 재시도 한도 내에 성공하면 저장되고 알림은 발행하지 않는다")
    void syncNextEvents_재시도_끝에_성공() {
        given(sportsDbProperties.getTargetLeagueIds()).willReturn(List.of(LEAGUE_1));
        given(sportsDbApiClient.getNextEventsByLeague(LEAGUE_1))
            .willThrow(new SportsDbApiException("일시적 오류"))
            .willThrow(new SportsDbApiException("일시적 오류"))
            .willReturn(sampleResponse());

        sportsDbContentSyncService.syncNextEvents();

        then(sportsDbApiClient).should(times(3)).getNextEventsByLeague(LEAGUE_1);
        then(sportsDbContentPersister).should().persistEvents(sampleResponse().events());
        then(eventPublisher).should(never()).publishEvent(any());
    }

    @Test
    @DisplayName("재시도 한도를 넘겨 계속 실패해도 다른 리그는 계속 처리되고, 실패한 리그는 관리자에게 알림이 간다")
    void syncNextEvents_한_리그_완전_실패해도_나머지_리그는_처리되고_알림이_발행된다() {
        given(sportsDbProperties.getTargetLeagueIds()).willReturn(List.of(LEAGUE_1, LEAGUE_2));
        given(sportsDbApiClient.getNextEventsByLeague(LEAGUE_1))
            .willThrow(new SportsDbApiException("영구 오류"));
        given(sportsDbApiClient.getNextEventsByLeague(LEAGUE_2)).willReturn(sampleResponse());

        User admin = new User("admin@test.com", "Admin", "pw", null, UserRole.ADMIN);
        UUID adminId = UUID.randomUUID();
        ReflectionTestUtils.setField(admin, "id", adminId);
        given(userRepository.findByRole(UserRole.ADMIN)).willReturn(List.of(admin));

        sportsDbContentSyncService.syncNextEvents();

        // 실패한 리그는 재시도 한도(3회)만큼 호출된다
        then(sportsDbApiClient).should(times(3)).getNextEventsByLeague(LEAGUE_1);
        // 실패와 무관하게 다음 리그는 정상 처리된다
        then(sportsDbApiClient).should(times(1)).getNextEventsByLeague(LEAGUE_2);
        then(sportsDbContentPersister).should().persistEvents(sampleResponse().events());

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        then(eventPublisher).should().publishEvent(captor.capture());
        NotificationEvent event = captor.getValue();
        Assertions.assertThat(event.receiverId()).isEqualTo(adminId);
        Assertions.assertThat(event.level()).isEqualTo(NotificationLevel.WARNING);
        Assertions.assertThat(event.content()).contains(LEAGUE_1);
    }

    @Test
    @DisplayName("관리자가 없으면 실패해도 알림을 발행하지 않는다")
    void syncNextEvents_관리자가_없으면_알림_미발행() {
        given(sportsDbProperties.getTargetLeagueIds()).willReturn(List.of(LEAGUE_1));
        given(sportsDbApiClient.getNextEventsByLeague(LEAGUE_1))
            .willThrow(new SportsDbApiException("영구 오류"));
        given(userRepository.findByRole(UserRole.ADMIN)).willReturn(List.of());

        sportsDbContentSyncService.syncNextEvents();

        then(eventPublisher).should(never()).publishEvent(any());
    }

    private SportsDbEventsResponse sampleResponse() {
        SportsDbEvent event = new SportsDbEvent(
            "1", "샘플 경기", "2026-08-01", "샘플 리그", "샘플 경기장", "https://sample.jpg");
        return new SportsDbEventsResponse(List.of(event));
    }
}
