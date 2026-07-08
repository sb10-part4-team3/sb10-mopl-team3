package com.example.sb10_MoPl_team3.tmdb.batch.movie;

import com.example.sb10_MoPl_team3.tmdb.TmdbConstants;
import com.example.sb10_MoPl_team3.tmdb.cache.TmdbGenreCache;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbMoviePopularResponse.TmdbMovieResult;
import com.example.sb10_MoPl_team3.tmdb.mapper.TmdbContentMapper;
import com.example.sb10_MoPl_team3.tmdb.service.SyncPayload;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TmdbMoviePayloadProcessor implements ItemProcessor<TmdbMovieResult, SyncPayload> {

  private final TmdbGenreCache tmdbGenreCache;
  private final TmdbContentMapper tmdbContentMapper;

  @Override
  public SyncPayload process(TmdbMovieResult result) {
    Map<Integer, String> genreMap = tmdbGenreCache.getMovieGenres();

    List<String> genreNames = result.genreIds().stream()
        .map(id -> genreMap.getOrDefault(id, "기타"))
        .toList();

    return new SyncPayload(
        TmdbConstants.externalId("MOVIE", result.id()),
        result.title(),
        result.overview(),
        result.posterPath(),
        genreNames,
        () -> tmdbContentMapper.toContent(result)
    );
  }
}