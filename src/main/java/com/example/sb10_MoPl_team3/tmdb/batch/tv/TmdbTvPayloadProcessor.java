package com.example.sb10_MoPl_team3.tmdb.batch.tv;

import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.tmdb.batch.TmdbPayloadProcessorSupport;
import com.example.sb10_MoPl_team3.tmdb.cache.TmdbGenreCache;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbTvPopularResponse.TmdbTvResult;
import com.example.sb10_MoPl_team3.tmdb.mapper.TmdbContentMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TmdbTvPayloadProcessor extends TmdbPayloadProcessorSupport<TmdbTvResult> {

  private final TmdbGenreCache tmdbGenreCache;
  private final TmdbContentMapper tmdbContentMapper;

  @Override
  protected Map<Integer, String> getGenreMap() {
    return tmdbGenreCache.getTvGenres();
  }

  @Override
  protected String externalIdPrefix() {
    return "TV";
  }

  @Override
  protected long id(TmdbTvResult result) {
    return result.id();
  }

  @Override
  protected String title(TmdbTvResult result) {
    return result.name();
  }

  @Override
  protected String overview(TmdbTvResult result) {
    return result.overview();
  }

  @Override
  protected String posterPath(TmdbTvResult result) {
    return result.posterPath();
  }

  @Override
  protected List<Integer> genreIds(TmdbTvResult result) {
    return result.genreIds();
  }

  @Override
  protected Content toContent(TmdbTvResult result) {
    return tmdbContentMapper.toContent(result);
  }
}
