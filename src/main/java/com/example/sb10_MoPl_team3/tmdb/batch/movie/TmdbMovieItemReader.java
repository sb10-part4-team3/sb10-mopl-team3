package com.example.sb10_MoPl_team3.tmdb.batch.movie;

import com.example.sb10_MoPl_team3.tmdb.TmdbConstants;
import com.example.sb10_MoPl_team3.tmdb.client.TmdbApiClient;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbMoviePopularResponse;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbMoviePopularResponse.TmdbMovieResult;
import java.util.Iterator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class TmdbMovieItemReader implements ItemReader<TmdbMovieResult> {

  private final TmdbApiClient tmdbApiClient;

  @Value("${tmdb.max-pages:0}")
  private int maxPages; // 0이면 제한 없음 (운영용), 양수면 그 페이지까지만 (테스트용)

  private int currentPage = 1;
  private int totalPages = Integer.MAX_VALUE;
  private Iterator<TmdbMovieResult> currentIterator;

  @Override
  public TmdbMovieResult read() {
    if (currentIterator != null && currentIterator.hasNext()) {
      return currentIterator.next();
    }

    int pageLimit = resolvePageLimit();

    while (currentPage <= pageLimit) {
      TmdbMoviePopularResponse response = tmdbApiClient.getPopularMovies(currentPage);
      totalPages = response.totalPages();
      pageLimit = resolvePageLimit();

      List<TmdbMovieResult> results = response.results();
      currentIterator = results.iterator();
      currentPage++;

      if (currentIterator.hasNext()) {
        return currentIterator.next();
      }
    }

    log.info("TMDB 영화 수집 완료. 총 {} 페이지 처리", currentPage - 1);
    return null;
  }

  private int resolvePageLimit() {
    int limit = Math.min(totalPages, TmdbConstants.MAX_PAGE);
    return (maxPages > 0) ? Math.min(maxPages, limit) : limit;
  }
}