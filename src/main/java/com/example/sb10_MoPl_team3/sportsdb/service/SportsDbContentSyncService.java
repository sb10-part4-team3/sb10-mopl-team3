package com.example.sb10_MoPl_team3.sportsdb.service;

import com.example.sb10_MoPl_team3.notification.enums.NotificationLevel;
import com.example.sb10_MoPl_team3.notification.event.NotificationEvent;
import com.example.sb10_MoPl_team3.sportsdb.client.SportsDbApiClient;
import com.example.sb10_MoPl_team3.sportsdb.config.SportsDbProperties;
import com.example.sb10_MoPl_team3.sportsdb.dto.SportsDbEventsResponse;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SportsDbContentSyncService {

  private static final int MAX_ATTEMPTS = 3;
  private static final String NOTIFICATION_TITLE = "SportsDB 동기화 일부 실패";

  private final SportsDbApiClient sportsDbApiClient;
  private final SportsDbProperties sportsDbProperties;
  private final SportsDbContentPersister sportsDbContentPersister;
  private final ApplicationEventPublisher eventPublisher;
  private final UserRepository userRepository;

  public void syncNextEvents() {
    syncByLeague("다음 경기", sportsDbApiClient::getNextEventsByLeague);
  }

  public void syncPastEvents() {
    syncByLeague("지난 경기", sportsDbApiClient::getPastEventsByLeague);
  }

  private void syncByLeague(String syncType, Function<String, SportsDbEventsResponse> fetcher) {
    List<String> failedLeagueIds = new ArrayList<>();

    for (String leagueId : sportsDbProperties.getTargetLeagueIds()) {
      try {
        SportsDbEventsResponse response = fetchWithRetry(fetcher, leagueId);
        sportsDbContentPersister.persistEvents(response.events());
      } catch (Exception e) {
        log.warn("리그 동기화 실패, leagueId={}", leagueId, e);
        failedLeagueIds.add(leagueId);
      }
    }

    if (!failedLeagueIds.isEmpty()) {
      notifyPartialFailure(syncType, failedLeagueIds);
    }
  }

  private SportsDbEventsResponse fetchWithRetry(
      Function<String, SportsDbEventsResponse> fetcher,
      String leagueId
  ) {
    RuntimeException lastException = null;
    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      try {
        return fetcher.apply(leagueId);
      } catch (RuntimeException e) {
        lastException = e;
        log.debug("리그 동기화 재시도 {}/{}, leagueId={}", attempt, MAX_ATTEMPTS, leagueId, e);
      }
    }
    throw lastException;
  }

  private void notifyPartialFailure(String syncType, List<String> failedLeagueIds) {
    String content = "SportsDB %s 동기화 중 일부 리그 동기화에 실패했습니다. leagueIds=%s"
        .formatted(syncType, failedLeagueIds);

    for (User admin : userRepository.findByRole(UserRole.ADMIN)) {
      eventPublisher.publishEvent(new NotificationEvent(
          admin.getId(),
          NOTIFICATION_TITLE,
          content,
          NotificationLevel.WARNING));
    }
  }
}
