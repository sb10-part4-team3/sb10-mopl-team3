package com.example.sb10_MoPl_team3.sportsdb.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sb10_MoPl_team3.sportsdb.dto.SportsDbEventsResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Tag("external")
class SportsDbApiClientIntegrationTest {

  private static final String EPL_LEAGUE_ID = "4328";

  @Autowired
  private SportsDbApiClient sportsDbApiClient;

  @Test
  @DisplayName("EPL의 다음 경기 목록을 요청하면 비어있지 않은 이벤트 목록을 반환한다")
  void getNextEventsByLeague_EPL_returnsNonEmptyEvents() {
    SportsDbEventsResponse response = sportsDbApiClient.getNextEventsByLeague(EPL_LEAGUE_ID);

    assertThat(response).isNotNull();
    assertThat(response.events()).isNotEmpty();
    assertThat(response.events()).allSatisfy(event -> assertThat(event.eventName()).isNotBlank());
  }

  @Test
  @DisplayName("EPL의 지난 경기 목록을 요청하면 비어있지 않은 이벤트 목록을 반환한다")
  void getPastEventsByLeague_EPL_returnsNonEmptyEvents() {
    SportsDbEventsResponse response = sportsDbApiClient.getPastEventsByLeague(EPL_LEAGUE_ID);

    assertThat(response).isNotNull();
    assertThat(response.events()).isNotEmpty();
    assertThat(response.events()).allSatisfy(event -> assertThat(event.eventName()).isNotBlank());
  }
}
