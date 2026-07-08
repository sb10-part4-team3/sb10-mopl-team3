package com.example.sb10_MoPl_team3.tmdb.batch;

import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.tmdb.TmdbConstants;
import com.example.sb10_MoPl_team3.tmdb.service.SyncPayload;
import java.util.List;
import java.util.Map;
import org.springframework.batch.item.ItemProcessor;

public abstract class TmdbPayloadProcessorSupport<R> implements ItemProcessor<R, SyncPayload> {

  @Override
  public SyncPayload process(R result) {
    Map<Integer, String> genreMap = getGenreMap();

    List<String> genreNames = genreIds(result).stream()
        .map(genreId -> genreMap.getOrDefault(genreId, "기타"))
        .toList();

    return new SyncPayload(
        TmdbConstants.externalId(externalIdPrefix(), id(result)),
        title(result),
        overview(result),
        posterPath(result),
        genreNames,
        () -> toContent(result)
    );
  }

  protected abstract Map<Integer, String> getGenreMap();

  protected abstract String externalIdPrefix();

  protected abstract long id(R result);

  protected abstract String title(R result);

  protected abstract String overview(R result);

  protected abstract String posterPath(R result);

  protected abstract List<Integer> genreIds(R result);

  protected abstract Content toContent(R result);
}
