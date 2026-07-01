package com.example.sb10_MoPl_team3.tmdb.cache;

import com.example.sb10_MoPl_team3.tmdb.client.TmdbApiClient;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbGenreListResponse;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbGenreListResponse.TmdbGenre;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TmdbGenreCache {

  private final TmdbApiClient tmdbApiClient;

  private Map<Integer, String> movieGenres;
  private Map<Integer, String> tvGenres;

  @PostConstruct
  void init() {
    this.movieGenres = toMap(tmdbApiClient.getMovieGenres());
    this.tvGenres = toMap(tmdbApiClient.getTvGenres());
  }

  public String getMovieGenreName(int genreId) {
    return movieGenres.getOrDefault(genreId, "기타");
  }

  public String getTvGenreName(int genreId) {
    return tvGenres.getOrDefault(genreId, "기타");
  }

  private Map<Integer, String> toMap(TmdbGenreListResponse response) {
    return response.genres().stream()
        .collect(Collectors.toMap(TmdbGenre::id, TmdbGenre::name));
  }
}