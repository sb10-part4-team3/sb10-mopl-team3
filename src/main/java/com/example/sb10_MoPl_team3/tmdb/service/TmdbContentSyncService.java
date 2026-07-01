package com.example.sb10_MoPl_team3.tmdb.service;

import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.repository.ContentRepository;
import com.example.sb10_MoPl_team3.content.service.ContentTagService;
import com.example.sb10_MoPl_team3.tmdb.cache.TmdbGenreCache;
import com.example.sb10_MoPl_team3.tmdb.client.TmdbApiClient;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbMoviePopularResponse;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbMoviePopularResponse.TmdbMovieResult;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbTvPopularResponse;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbTvPopularResponse.TmdbTvResult;
import com.example.sb10_MoPl_team3.tmdb.mapper.TmdbContentMapper;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TmdbContentSyncService {

  private final TmdbApiClient tmdbApiClient;
  private final TmdbContentMapper tmdbContentMapper;
  private final ContentRepository contentRepository;
  private final ContentTagService contentTagService;
  private final TmdbGenreCache tmdbGenreCache;

  @Transactional
  public void syncPopularMovies(int page) {
    TmdbMoviePopularResponse response = tmdbApiClient.getPopularMovies(page);

    for (TmdbMovieResult result : response.results()) {
      upsert(result);
    }
  }

  @Transactional
  public void syncPopularTvs(int page) {
    TmdbTvPopularResponse response = tmdbApiClient.getPopularTvs(page);
    for (TmdbTvResult result : response.results()) {
      upsert(result);
    }
  }

  private void upsert(TmdbMovieResult result) {
    String externalId = "MOVIE-" + result.id();
    String source = "TMDB";

    Content content = contentRepository.findByExternalIdAndSource(externalId, source)
        .map(existing -> {
          existing.syncFromExternal(
              result.title(),
              result.overview(),
              toFullImageUrl(result.posterPath())
          );
          return existing;
        })
        .orElseGet(() -> contentRepository.save(tmdbContentMapper.toContent(result)));

    List<String> tagNames = result.genreIds().stream()
        .map(tmdbGenreCache::getMovieGenreName)
        .toList();

    contentTagService.syncTags(content, tagNames);
  }

  private void upsert(TmdbTvResult result) {
    String externalId = "TV-" + result.id();
    String source = "TMDB";

    Content content = contentRepository.findByExternalIdAndSource(externalId, source)
        .map(existing -> {
          existing.syncFromExternal(
              result.name(),
              result.overview(),
              toFullImageUrl(result.posterPath())
          );
          return existing;
        })
        .orElseGet(() -> contentRepository.save(tmdbContentMapper.toContent(result)));

    List<String> tagNames = result.genreIds().stream()
        .map(tmdbGenreCache::getTvGenreName)
        .toList();

    contentTagService.syncTags(content, tagNames);
  }

  private String toFullImageUrl(String posterPath) {
    if (posterPath == null || posterPath.isBlank()) {
      return null;
    }
    return "https://image.tmdb.org/t/p/w500" + posterPath;
  }
}