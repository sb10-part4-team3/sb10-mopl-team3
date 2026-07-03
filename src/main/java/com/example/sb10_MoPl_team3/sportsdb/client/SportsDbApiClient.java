package com.example.sb10_MoPl_team3.sportsdb.client;

import com.example.sb10_MoPl_team3.global.exception.SportsDbApiException;
import com.example.sb10_MoPl_team3.sportsdb.dto.SportsDbEventsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class SportsDbApiClient {

  private final RestClient sportsDbRestClient;

  public SportsDbEventsResponse getNextEventsByLeague(String leagueId) {
    return sportsDbRestClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/eventsnextleague.php")
            .queryParam("id", leagueId)
            .build())
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
          throw new SportsDbApiException("SportsDB 요청 실패: " + res.getStatusCode());
        })
        .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
          throw new SportsDbApiException("SportsDB 서버 오류: " + res.getStatusCode());
        })
        .body(SportsDbEventsResponse.class);
  }

  public SportsDbEventsResponse getPastEventsByLeague(String leagueId) {
    return sportsDbRestClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/eventspastleague.php")
            .queryParam("id", leagueId)
            .build())
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
          throw new SportsDbApiException("SportsDB 요청 실패: " + res.getStatusCode());
        })
        .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
          throw new SportsDbApiException("SportsDB 서버 오류: " + res.getStatusCode());
        })
        .body(SportsDbEventsResponse.class);
  }
}