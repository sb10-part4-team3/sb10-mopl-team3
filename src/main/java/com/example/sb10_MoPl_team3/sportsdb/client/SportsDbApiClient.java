package com.example.sb10_MoPl_team3.sportsdb.client;

import com.example.sb10_MoPl_team3.global.exception.SportsDbApiException;
import com.example.sb10_MoPl_team3.sportsdb.dto.SportsDbEventsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class SportsDbApiClient {

  private final RestClient sportsDbRestClient;

  private SportsDbEventsResponse fetchEvents(String path, String leagueId) {
    try {
      return sportsDbRestClient.get()
          .uri(uriBuilder -> uriBuilder.path(path).queryParam("id", leagueId).build())
          .retrieve()
          .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
            throw new SportsDbApiException("SportsDB 요청 실패: " + res.getStatusCode());
          })
          .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
            throw new SportsDbApiException("SportsDB 서버 오류: " + res.getStatusCode());
          })
          .body(SportsDbEventsResponse.class);
    } catch (SportsDbApiException e) {
      throw e;
    } catch (RestClientException e) {
      throw new SportsDbApiException("SportsDB 요청 중 네트워크 오류 발생 (leagueId=" + leagueId + ")", e);
    }
  }

  public SportsDbEventsResponse getNextEventsByLeague(String leagueId) {
    return fetchEvents("/eventsnextleague.php", leagueId);
  }

  public SportsDbEventsResponse getPastEventsByLeague(String leagueId) {
    return fetchEvents("/eventspastleague.php", leagueId);
  }
}