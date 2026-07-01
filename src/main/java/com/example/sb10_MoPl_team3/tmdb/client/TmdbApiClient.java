package com.example.sb10_MoPl_team3.tmdb.client;

import com.example.sb10_MoPl_team3.global.exception.TmdbApiException;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbGenreListResponse;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbMoviePopularResponse;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbTvPopularResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class TmdbApiClient {

  private final RestClient tmdbRestClient;

  public TmdbMoviePopularResponse getPopularMovies(int page) {
    return tmdbRestClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/movie/popular")
            .queryParam("language", "ko-KR")
            .queryParam("page", page)
            .build())
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
          throw new TmdbApiException("TMDB 요청 실패: " + res.getStatusCode());
        })
        .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
          throw new TmdbApiException("TMDB 서버 오류: " + res.getStatusCode());
        })
        .body(TmdbMoviePopularResponse.class);
  }

  public TmdbTvPopularResponse getPopularTvs(int page){
    return tmdbRestClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/tv/popular")
            .queryParam("language", "ko-KR")
            .queryParam("page", page)
            .build())
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
          throw new TmdbApiException("TMDB 요청 실패: " + res.getStatusCode());
        })
        .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
          throw new TmdbApiException("TMDB 서버 오류: " + res.getStatusCode());
        })
        .body(TmdbTvPopularResponse.class);
  }

  public TmdbGenreListResponse getMovieGenres() {
    return tmdbRestClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/genre/movie/list")
            .queryParam("language", "ko-KR")
            .build())
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
          throw new TmdbApiException("TMDB 요청 실패: " + res.getStatusCode());
        })
        .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
          throw new TmdbApiException("TMDB 서버 오류: " + res.getStatusCode());
        })
        .body(TmdbGenreListResponse.class);
  }

  public TmdbGenreListResponse getTvGenres() {
    return tmdbRestClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/genre/tv/list")
            .queryParam("language", "ko-KR")
            .build())
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
          throw new TmdbApiException("TMDB 요청 실패: " + res.getStatusCode());
        })
        .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
          throw new TmdbApiException("TMDB 서버 오류: " + res.getStatusCode());
        })
        .body(TmdbGenreListResponse.class);
  }
}
