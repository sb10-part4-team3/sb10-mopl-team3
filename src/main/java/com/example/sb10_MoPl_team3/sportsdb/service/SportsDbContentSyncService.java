package com.example.sb10_MoPl_team3.sportsdb.service;

import com.example.sb10_MoPl_team3.sportsdb.client.SportsDbApiClient;
import com.example.sb10_MoPl_team3.sportsdb.config.SportsDbProperties;
import com.example.sb10_MoPl_team3.sportsdb.dto.SportsDbEventsResponse;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SportsDbContentSyncService {

  private final SportsDbApiClient sportsDbApiClient;
  private final SportsDbProperties sportsDbProperties;
  private final SportsDbContentPersister sportsDbContentPersister;

  public void syncNextEvents() {
    syncByLeague(sportsDbApiClient::getNextEventsByLeague);
  }

  public void syncPastEvents() {
    syncByLeague(sportsDbApiClient::getPastEventsByLeague);
  }

  private void syncByLeague(Function<String, SportsDbEventsResponse> fetcher) {
    for (String leagueId : sportsDbProperties.getTargetLeagueIds()) {
      try {
        SportsDbEventsResponse response = fetcher.apply(leagueId);
        sportsDbContentPersister.persistEvents(response.events());
      } catch (Exception e) {
        log.warn("리그 동기화 실패, leagueId={}", leagueId, e);
      }
    }
  }
}