package com.example.sb10_MoPl_team3.tmdb.cache;

import com.example.sb10_MoPl_team3.tmdb.client.TmdbApiClient;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbGenreListResponse;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbGenreListResponse.TmdbGenre;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TmdbGenreCache {

  private final TmdbApiClient tmdbApiClient;

  private volatile Map<Integer, String> movieGenres;
  private volatile Map<Integer, String> tvGenres;

  public Map<Integer, String> getMovieGenres() {
    if (movieGenres == null) {
      synchronized (this) {
        if (movieGenres == null) {
          movieGenres = toMap(tmdbApiClient.getMovieGenres());
        }
      }
    }
    return movieGenres;
  }

  public Map<Integer, String> getTvGenres() {
    if (tvGenres == null) {
      synchronized (this) {
        if (tvGenres == null) {
          tvGenres = toMap(tmdbApiClient.getTvGenres());
        }
      }
    }
    return tvGenres;
  }

  private Map<Integer, String> toMap(TmdbGenreListResponse response) {
    return response.genres().stream()
        .collect(Collectors.toMap(TmdbGenre::id, TmdbGenre::name));
  }
}