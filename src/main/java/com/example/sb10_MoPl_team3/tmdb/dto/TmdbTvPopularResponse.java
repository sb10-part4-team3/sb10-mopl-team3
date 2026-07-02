package com.example.sb10_MoPl_team3.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record TmdbTvPopularResponse(
    int page,
    List<TmdbTvResult> results,
    @JsonProperty("total_pages") int totalPages,
    @JsonProperty("total_results") int totalResults
) {
  public record TmdbTvResult(
      long id,
      String name,
      @JsonProperty("original_name") String originalName,
      String overview,
      @JsonProperty("poster_path") String posterPath,
      @JsonProperty("backdrop_path") String backdropPath,
      @JsonProperty("first_air_date") String firstAirDate,
      double popularity,
      @JsonProperty("vote_average") double voteAverage,
      @JsonProperty("vote_count") int voteCount,
      @JsonProperty("genre_ids") List<Integer> genreIds,
      @JsonProperty("origin_country") List<String> originCountry
  ) {}
}