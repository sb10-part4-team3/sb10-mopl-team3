package com.example.sb10_MoPl_team3.tmdb.dto;

import java.util.List;

public record TmdbGenreListResponse(
    List<TmdbGenre> genres
) {
  public record TmdbGenre(
      int id,
      String name
  ) {}
}