package com.example.sb10_MoPl_team3.tmdb.batch.tv;

import com.example.sb10_MoPl_team3.tmdb.TmdbConstants;
import com.example.sb10_MoPl_team3.tmdb.cache.TmdbGenreCache;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbTvPopularResponse.TmdbTvResult;
import com.example.sb10_MoPl_team3.tmdb.mapper.TmdbContentMapper;
import com.example.sb10_MoPl_team3.tmdb.service.SyncPayload;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TmdbTvPayloadProcessor implements ItemProcessor<TmdbTvResult, SyncPayload> {

  private final TmdbGenreCache tmdbGenreCache;
  private final TmdbContentMapper tmdbContentMapper;

  @Override
  public SyncPayload process(TmdbTvResult result) {
    Map<Integer, String> genreMap = tmdbGenreCache.getTvGenres();

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