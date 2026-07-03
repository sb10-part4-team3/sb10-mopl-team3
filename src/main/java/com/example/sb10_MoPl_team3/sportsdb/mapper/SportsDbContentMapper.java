package com.example.sb10_MoPl_team3.sportsdb.mapper;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.sportsdb.SportsDbConstants;
import com.example.sb10_MoPl_team3.sportsdb.dto.SportsDbEventsResponse.SportsDbEvent;
import org.springframework.stereotype.Component;

@Component
public class SportsDbContentMapper {

  public Content toContent(SportsDbEvent event) {
    return Content.builder()
        .type(ContentType.SPORT)
        .title(event.eventName())
        .description(buildDescription(event))
        .thumbnailUrl(event.thumbnail())
        .externalId(SportsDbConstants.externalId(event.idEvent()))
        .source(SportsDbConstants.SOURCE_SPORTS_DB)
        .build();
  }

  private String buildDescription(SportsDbEvent event) {
    return event.league() + " " + event.dateEvent();
  }
}
