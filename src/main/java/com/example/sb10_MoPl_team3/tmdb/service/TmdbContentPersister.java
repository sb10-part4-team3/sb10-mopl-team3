package com.example.sb10_MoPl_team3.tmdb.service;

import com.example.sb10_MoPl_team3.tmdb.TmdbConstants;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbMoviePopularResponse.TmdbMovieResult;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbTvPopularResponse.TmdbTvResult;
import com.example.sb10_MoPl_team3.tmdb.mapper.TmdbContentMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TmdbContentPersister {

  private final TmdbContentMapper tmdbContentMapper;
  private final TmdbContentUpsertExecutor tmdbContentUpsertExecutor;

  public void persistMovies(List<TmdbMovieResult> results, Map<Integer, String> genreMap) {
    for (TmdbMovieResult result : results) {
      try {
        tmdbContentUpsertExecutor.upsert(toPayload(result, genreMap));
      } catch (Exception e) {
        log.warn("영화 upsert 실패, externalId=MOVIE-{}", result.id(), e);
      }
    }
  }

  public void persistTvs(List<TmdbTvResult> results, Map<Integer, String> genreMap) {
    for (TmdbTvResult result : results) {
      try {
        tmdbContentUpsertExecutor.upsert(toPayload(result, genreMap));
      } catch (Exception e) {
        log.warn("TV upsert 실패, externalId=TV-{}", result.id(), e);
      }
    }
  }

  private SyncPayload toPayload(TmdbMovieResult result, Map<Integer, String> genreMap) {
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

  private SyncPayload toPayload(TmdbTvResult result, Map<Integer, String> genreMap) {
    List<String> genreNames = result.genreIds().stream()
        .map(id -> genreMap.getOrDefault(id, "기타"))
        .toList();

    return new SyncPayload(
        TmdbConstants.externalId("TV", result.id()),
        result.name(),
        result.overview(),
        result.posterPath(),
        genreNames,
        () -> tmdbContentMapper.toContent(result)
    );
  }
}