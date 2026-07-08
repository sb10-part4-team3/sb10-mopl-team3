package com.example.sb10_MoPl_team3.tmdb.batch.tv;

import com.example.sb10_MoPl_team3.tmdb.client.TmdbApiClient;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbTvPopularResponse;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbTvPopularResponse.TmdbTvResult;
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
public class TmdbTvItemReader implements ItemReader<TmdbTvResult> {

  private final TmdbApiClient tmdbApiClient;

  @Value("${tmdb.max-pages:0}")
  private int maxPages;

  private int currentPage = 1;
  private int totalPages = Integer.MAX_VALUE;
  private Iterator<TmdbTvResult> currentIterator;

  @Override
  public TmdbTvResult read() {
    if (currentIterator != null && currentIterator.hasNext()) {
      return currentIterator.next();
    }

    int pageLimit = (maxPages > 0) ? Math.min(maxPages, totalPages) : totalPages;

    while (currentPage <= pageLimit) {
      TmdbTvPopularResponse response = tmdbApiClient.getPopularTvs(currentPage);
      totalPages = response.totalPages();
      pageLimit = (maxPages > 0) ? Math.min(maxPages, totalPages) : totalPages;

      List<TmdbTvResult> results = response.results();
      if (results == null || results.isEmpty()) {
        currentPage++;
        continue;
      }
      currentIterator = results.iterator();
      currentPage++;

      if (currentIterator.hasNext()) {
        return currentIterator.next();
      }
    }

    log.info("TMDB TV 수집 완료. 총 {} 페이지 처리", currentPage - 1);
    return null;
  }
}