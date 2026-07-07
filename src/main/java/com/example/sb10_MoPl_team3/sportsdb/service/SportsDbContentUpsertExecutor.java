package com.example.sb10_MoPl_team3.sportsdb.service;

import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.entity.ContentStats;
import com.example.sb10_MoPl_team3.content.repository.ContentRepository;
import com.example.sb10_MoPl_team3.content.repository.ContentStatsRepository;
import com.example.sb10_MoPl_team3.content.service.ContentTagService;
import com.example.sb10_MoPl_team3.sportsdb.SportsDbConstants;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SportsDbContentUpsertExecutor {

  private final ContentRepository contentRepository;
  private final ContentTagService contentTagService;
  private final ContentStatsRepository contentStatsRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void upsert(SportsDbSyncPayload payload) {
    boolean isSoftDeleted = contentRepository.existsDeletedByExternalIdAndSource(
        payload.externalId(), SportsDbConstants.SOURCE_SPORTS_DB
    );

    if (isSoftDeleted) {
      return;
    }

    Content content = contentRepository.findByExternalIdAndSource(payload.externalId(),
            SportsDbConstants.SOURCE_SPORTS_DB)
        .map(existing -> {
          existing.syncFromExternal(
              payload.title(),
              payload.description(),
              payload.thumbnailUrl(),
              payload.eventDate()
          );
          return existing;
        })
        .orElseGet(() -> {
          Content newContent = contentRepository.save(payload.newContentSupplier().get());
          contentStatsRepository.save(ContentStats.createDefault(newContent));
          return newContent;
        });

    contentTagService.syncTags(content, payload.tagNames());
  }
}