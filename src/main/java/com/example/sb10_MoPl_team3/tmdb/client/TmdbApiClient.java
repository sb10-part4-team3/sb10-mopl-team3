package com.example.sb10_MoPl_team3.tmdb.client;

import com.example.sb10_MoPl_team3.global.exception.TmdbApiException;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbGenreListResponse;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbMoviePopularResponse;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbTvPopularResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class TmdbApiClient {

  private final RestClient tmdbRestClient;

  @Value("${tmdb.access-token:}")
  private String accessToken;

  public TmdbMoviePopularResponse getPopularMovies(int page) {
    validateAccessToken();
    return executeRequest(
        tmdbRestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/movie/popular")
                .queryParam("language", "ko-KR")
                .queryParam("page", page)
                .build()),
        "TMDB 인기 영화 조회 실패: ",
        TmdbMoviePopularResponse.class);
  }

  public TmdbTvPopularResponse getPopularTvs(int page) {
    validateAccessToken();
    return executeRequest(
        tmdbRestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/tv/popular")
                .queryParam("language", "ko-KR")
                .queryParam("page", page)
                .build()),
        "TMDB 인기 TV 조회 실패: ",
        TmdbTvPopularResponse.class);
  }

  public TmdbGenreListResponse getMovieGenres() {
    validateAccessToken();
    return executeRequest(
        tmdbRestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/genre/movie/list")
                .queryParam("language", "ko-KR")
                .build()),
        "TMDB 영화 장르 조회 실패: ",
        TmdbGenreListResponse.class);
  }

  public TmdbGenreListResponse getTvGenres() {
    validateAccessToken();
    return executeRequest(
        tmdbRestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/genre/tv/list")
                .queryParam("language", "ko-KR")
                .build()),
        "TMDB TV 장르 조회 실패: ",
        TmdbGenreListResponse.class);
  }

  private <T> T executeRequest(
      RestClient.RequestHeadersSpec<?> spec,
      String errorMessage,
      Class<T> responseType
  ) {
    try {
      return spec.retrieve()
          .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
            throw new TmdbApiException(errorMessage + "TMDB 요청 실패: " + res.getStatusCode());
          })
          .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
            throw new TmdbApiException(errorMessage + "TMDB 서버 오류: " + res.getStatusCode());
          })
          .body(responseType);
    } catch (RestClientException e) {
      throw new TmdbApiException(errorMessage + e.getMessage(), e);
    }
  }

  private void validateAccessToken() {
    if (accessToken == null || accessToken.isBlank()) {
      throw new TmdbApiException("TMDB_ACCESS_TOKEN이 설정되지 않았습니다. TMDB 연동 기능을 사용할 수 없습니다.");
    }
  }
}
