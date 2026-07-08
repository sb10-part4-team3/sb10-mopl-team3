package com.example.sb10_MoPl_team3.tmdb.batch.movie;

import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.tmdb.batch.TmdbPayloadProcessorSupport;
import com.example.sb10_MoPl_team3.tmdb.cache.TmdbGenreCache;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbMoviePopularResponse.TmdbMovieResult;
import com.example.sb10_MoPl_team3.tmdb.mapper.TmdbContentMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TmdbMoviePayloadProcessor extends TmdbPayloadProcessorSupport<TmdbMovieResult> {

  private final TmdbGenreCache tmdbGenreCache;
  private final TmdbContentMapper tmdbContentMapper;

  @Override
  protected Map<Integer, String> getGenreMap() {
    return tmdbGenreCache.getMovieGenres();
  }

  @Override
  protected String externalIdPrefix() {
    return "MOVIE";
  }

  @Override
  protected long id(TmdbMovieResult result) {
    return result.id();
  }

  @Override
  protected String title(TmdbMovieResult result) {
    return result.title();
  }

  @Override
  protected String overview(TmdbMovieResult result) {
    return result.overview();
  }

  @Override
  protected String posterPath(TmdbMovieResult result) {
    return result.posterPath();
  }

  @Override
  protected List<Integer> genreIds(TmdbMovieResult result) {
    return result.genreIds();
  }

  @Override
  protected Content toContent(TmdbMovieResult result) {
    return tmdbContentMapper.toContent(result);
  }
}
