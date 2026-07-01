package com.example.sb10_MoPl_team3.tmdb.mapper;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbMoviePopularResponse.TmdbMovieResult;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbTvPopularResponse.TmdbTvResult;
import org.springframework.stereotype.Component;

@Component
public class TmdbContentMapper {

  private static final String TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500";
  private static final String SOURCE_TMDB = "TMDB";

  public Content toContent(TmdbMovieResult result) {
    return Content.builder()
        .type(ContentType.MOVIE)
        .title(result.title())
        .description(result.overview())
        .thumbnailUrl(toFullImageUrl(result.posterPath()))
        .externalId("MOVIE-" + result.id())
        .source(SOURCE_TMDB)
        .build();
  }

  public Content toContent(TmdbTvResult result) {
    return Content.builder()
        .type(ContentType.TV_SERIES)
        .title(result.name())
        .description(result.overview())
        .thumbnailUrl(toFullImageUrl(result.posterPath()))
        .externalId("TV-" + result.id())
        .source(SOURCE_TMDB)
        .build();
  }

  private String toFullImageUrl(String posterPath) {
    if (posterPath == null || posterPath.isBlank()) {
      return null;
    }
    return TMDB_IMAGE_BASE_URL + posterPath;
  }


}