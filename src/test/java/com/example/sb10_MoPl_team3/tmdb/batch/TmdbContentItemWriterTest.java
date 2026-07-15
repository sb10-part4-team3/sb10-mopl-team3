package com.example.sb10_MoPl_team3.tmdb.batch;

import static org.mockito.BDDMockito.then;

import com.example.sb10_MoPl_team3.tmdb.service.SyncPayload;
import com.example.sb10_MoPl_team3.tmdb.service.TmdbContentUpsertExecutor;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;

@ExtendWith(MockitoExtension.class)
class TmdbContentItemWriterTest {

  @Mock
  private TmdbContentUpsertExecutor tmdbContentUpsertExecutor;

  @InjectMocks
  private TmdbContentItemWriter tmdbContentItemWriter;

  @Test
  void write_청크의_각_항목을_순서대로_upsert에_위임한다() {
    SyncPayload first = payload("MOVIE-1");
    SyncPayload second = payload("MOVIE-2");

    tmdbContentItemWriter.write(new Chunk<>(List.of(first, second)));

    InOrder inOrder = Mockito.inOrder(tmdbContentUpsertExecutor);
    inOrder.verify(tmdbContentUpsertExecutor).upsert(first);
    inOrder.verify(tmdbContentUpsertExecutor).upsert(second);
    then(tmdbContentUpsertExecutor).shouldHaveNoMoreInteractions();
  }

  private SyncPayload payload(String externalId) {
    return new SyncPayload(externalId, "제목", "개요", "/poster.jpg", List.of("액션"), () -> null);
  }
}
