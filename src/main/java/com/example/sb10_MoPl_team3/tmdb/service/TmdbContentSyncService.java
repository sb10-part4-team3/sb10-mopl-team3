package com.example.sb10_MoPl_team3.tmdb.service;

import com.example.sb10_MoPl_team3.tmdb.client.TmdbApiClient;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbMoviePopularResponse;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbTvPopularResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TmdbContentSyncService {

  private final TmdbApiClient tmdbApiClient;
  private final TmdbContentPersister tmdbContentPersister;

  public void syncPopularMovies(int page) {
    TmdbMoviePopularResponse response = tmdbApiClient.getPopularMovies(page);  // 트랜잭션 밖
    tmdbContentPersister.persistMovies(response.results());  // 별도 빈 호출 → 프록시 정상 작동
  }

  public void syncPopularTvs(int page) {
    TmdbTvPopularResponse response = tmdbApiClient.getPopularTvs(page);
    tmdbContentPersister.persistTvs(response.results());
  }
}