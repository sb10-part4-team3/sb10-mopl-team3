package com.example.sb10_MoPl_team3.tmdb.service;

import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.entity.ContentStats;
import com.example.sb10_MoPl_team3.content.repository.ContentRepository;
import com.example.sb10_MoPl_team3.content.repository.ContentStatsRepository;
import com.example.sb10_MoPl_team3.content.service.ContentTagService;
import com.example.sb10_MoPl_team3.tmdb.TmdbConstants;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TmdbContentUpsertExecutor {

  private final ContentRepository contentRepository;
  private final ContentTagService contentTagService;
  private final ContentStatsRepository contentStatsRepository;

  public void upsert(SyncPayload payload) {
    boolean isSoftDeleted = contentRepository.existsDeletedByExternalIdAndSource(
        payload.externalId(), TmdbConstants.SOURCE_TMDB
    );

    if (isSoftDeleted) {
      return;
    }

    Content content = contentRepository.findByExternalIdAndSource(payload.externalId(),
            TmdbConstants.SOURCE_TMDB)
        .map(existing -> {
          existing.syncFromExternal(
              payload.title(),
              payload.overview(),
              TmdbConstants.toFullImageUrl(payload.posterPath()),
              null
          );
          return existing;
        })
        .orElseGet(() -> {
          Content newContent = contentRepository.save(payload.newContentSupplier().get());
          contentStatsRepository.save(ContentStats.createDefault(newContent));
          return newContent;
        });

    contentTagService.syncTags(content, payload.genreNames());
  }
}