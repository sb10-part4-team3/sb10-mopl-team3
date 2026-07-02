package com.example.sb10_MoPl_team3.tmdb.service;

import com.example.sb10_MoPl_team3.tmdb.cache.TmdbGenreCache;
import com.example.sb10_MoPl_team3.tmdb.client.TmdbApiClient;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbMoviePopularResponse;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbTvPopularResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TmdbContentSyncService {

  private final TmdbApiClient tmdbApiClient;
  private final TmdbGenreCache tmdbGenreCache;
  private final TmdbContentPersister tmdbContentPersister;

  public void syncPopularMovies(int page) {
    Map<Integer, String> genreMap = tmdbGenreCache.getMovieGenres();
    TmdbMoviePopularResponse response = tmdbApiClient.getPopularMovies(page);
    tmdbContentPersister.persistMovies(response.results(), genreMap);
  }

  public void syncPopularTvs(int page) {
    Map<Integer, String> genreMap = tmdbGenreCache.getTvGenres();
    TmdbTvPopularResponse response = tmdbApiClient.getPopularTvs(page);
    tmdbContentPersister.persistTvs(response.results(), genreMap);
  }
}