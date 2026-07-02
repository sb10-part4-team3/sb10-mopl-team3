package com.example.sb10_MoPl_team3.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record TmdbMoviePopularResponse(
    int page,
    List<TmdbMovieResult> results,
    @JsonProperty("total_pages") int totalPages,
    @JsonProperty("total_results") int totalResults
) {
  public record TmdbMovieResult(
      long id,
      String title,
      @JsonProperty("original_title") String originalTitle,
      String overview,
      @JsonProperty("poster_path") String posterPath,
      @JsonProperty("backdrop_path") String backdropPath,
      @JsonProperty("release_date") String releaseDate,
      double popularity,
      @JsonProperty("vote_average") double voteAverage,
      @JsonProperty("vote_count") int voteCount,
      @JsonProperty("genre_ids") List<Integer> genreIds,
      boolean adult
  ) {}
}