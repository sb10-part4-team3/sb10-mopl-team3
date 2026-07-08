package com.example.sb10_MoPl_team3.tmdb.batch;

import com.example.sb10_MoPl_team3.tmdb.service.SyncPayload;
import com.example.sb10_MoPl_team3.tmdb.service.TmdbContentUpsertExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TmdbContentItemWriter implements ItemWriter<SyncPayload> {

  private final TmdbContentUpsertExecutor tmdbContentUpsertExecutor;

  @Override
  public void write(Chunk<? extends SyncPayload> chunk) {
    for (SyncPayload payload : chunk) {
      tmdbContentUpsertExecutor.upsert(payload);
    }
  }
}