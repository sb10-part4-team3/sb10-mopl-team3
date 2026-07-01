package com.example.sb10_MoPl_team3.tmdb.service;

import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.repository.ContentRepository;
import com.example.sb10_MoPl_team3.content.service.ContentTagService;
import com.example.sb10_MoPl_team3.tmdb.TmdbConstants;
import com.example.sb10_MoPl_team3.tmdb.cache.TmdbGenreCache;
import com.example.sb10_MoPl_team3.tmdb.client.TmdbApiClient;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbMoviePopularResponse;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbMoviePopularResponse.TmdbMovieResult;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbTvPopularResponse;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbTvPopularResponse.TmdbTvResult;
import com.example.sb10_MoPl_team3.tmdb.mapper.TmdbContentMapper;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TmdbContentSyncService {

  private final TmdbApiClient tmdbApiClient;
  private final TmdbContentMapper tmdbContentMapper;
  private final ContentRepository contentRepository;
  private final TmdbGenreCache tmdbGenreCache;
  private final ContentTagService contentTagService;

  @Transactional
  public void syncPopularMovies(int page) {
    TmdbMoviePopularResponse response = tmdbApiClient.getPopularMovies(page);

    for (TmdbMovieResult result : response.results()) {
      upsertMovie(result);
    }
  }

  @Transactional
  public void syncPopularTvs(int page) {
    TmdbTvPopularResponse response = tmdbApiClient.getPopularTvs(page);

    for (TmdbTvResult result : response.results()) {
      upsertTv(result);
    }
  }

  private void upsertMovie(TmdbMovieResult result) {
    SyncPayload payload = new SyncPayload(
        TmdbConstants.externalId("MOVIE", result.id()),
        result.title(),
        result.overview(),
        result.posterPath(),
        result.genreIds(),
        tmdbGenreCache::getMovieGenreName,
        () -> tmdbContentMapper.toContent(result)
    );
    upsert(payload);
  }

  private void upsertTv(TmdbTvResult result) {
    SyncPayload payload = new SyncPayload(
        TmdbConstants.externalId("TV", result.id()),
        result.name(),
        result.overview(),
        result.posterPath(),
        result.genreIds(),
        tmdbGenreCache::getTvGenreName,
        () -> tmdbContentMapper.toContent(result)
    );
    upsert(payload);
  }

  private void upsert(SyncPayload payload) {
    Content content = contentRepository.findByExternalIdAndSource(payload.externalId(), TmdbConstants.SOURCE_TMDB)
        .map(existing -> {
          existing.syncFromExternal(
              payload.title(),
              payload.overview(),
              TmdbConstants.toFullImageUrl(payload.posterPath())
          );
          return existing;
        })
        .orElseGet(() -> contentRepository.save(payload.newContentSupplier().get()));

    List<String> tagNames = payload.genreIds().stream()
        .map(payload.genreResolver())
        .toList();

    contentTagService.syncTags(content, tagNames);
  }

  private record SyncPayload(
      String externalId,
      String title,
      String overview,
      String posterPath,
      List<Integer> genreIds,
      Function<Integer, String> genreResolver,
      java.util.function.Supplier<Content> newContentSupplier
  ) {}
}