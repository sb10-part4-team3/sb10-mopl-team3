package com.example.sb10_MoPl_team3.sportsdb.service;

import com.example.sb10_MoPl_team3.sportsdb.client.SportsDbApiClient;
import com.example.sb10_MoPl_team3.sportsdb.config.SportsDbProperties;
import com.example.sb10_MoPl_team3.sportsdb.dto.SportsDbEventsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SportsDbContentSyncService {

  private final SportsDbApiClient sportsDbApiClient;
  private final SportsDbProperties sportsDbProperties;
  private final SportsDbContentPersister sportsDbContentPersister;

  public void syncNextEvents() {
    for (String leagueId : sportsDbProperties.getTargetLeagueIds()) {
      SportsDbEventsResponse response = sportsDbApiClient.getNextEventsByLeague(leagueId);
      sportsDbContentPersister.persistEvents(response.events());
    }
  }

  public void syncPastEvents() {
    for (String leagueId : sportsDbProperties.getTargetLeagueIds()) {
      SportsDbEventsResponse response = sportsDbApiClient.getPastEventsByLeague(leagueId);
      sportsDbContentPersister.persistEvents(response.events());
    }
  }
}