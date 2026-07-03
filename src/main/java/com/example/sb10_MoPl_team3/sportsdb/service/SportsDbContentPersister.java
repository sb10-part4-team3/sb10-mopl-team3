package com.example.sb10_MoPl_team3.sportsdb.service;

import com.example.sb10_MoPl_team3.sportsdb.SportsDbConstants;
import com.example.sb10_MoPl_team3.sportsdb.dto.SportsDbEventsResponse.SportsDbEvent;
import com.example.sb10_MoPl_team3.sportsdb.mapper.SportsDbContentMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SportsDbContentPersister {

  private final SportsDbContentMapper sportsDbContentMapper;
  private final SportsDbContentUpsertExecutor sportsDbContentUpsertExecutor;

  public void persistEvents(List<SportsDbEvent> events) {
    for (SportsDbEvent event : events) {
      try {
        sportsDbContentUpsertExecutor.upsert(toPayload(event));
      } catch (Exception e) {
        log.warn("경기 upsert 실패, externalId=EVENT-{}", event.idEvent(), e);
      }
    }
  }

  private SportsDbSyncPayload toPayload(SportsDbEvent event) {
    List<String> tagNames = List.of("스포츠", "Soccer", event.venue());

    return new SportsDbSyncPayload(
        SportsDbConstants.externalId(event.idEvent()),
        event.eventName(),
        event.league() + " " + event.dateEvent(),
        event.thumbnail(),
        tagNames,
        () -> sportsDbContentMapper.toContent(event)
    );
  }
}