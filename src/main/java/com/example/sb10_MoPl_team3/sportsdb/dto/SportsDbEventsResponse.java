package com.example.sb10_MoPl_team3.sportsdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record SportsDbEventsResponse(
    List<SportsDbEvent> events
) {
  public record SportsDbEvent(
      @JsonProperty("idEvent") String idEvent,
      @JsonProperty("strEvent") String eventName,
      @JsonProperty("dateEvent") String dateEvent,
      @JsonProperty("strLeague") String league,
      @JsonProperty("strVenue") String venue,
      @JsonProperty("strThumb") String thumbnail
  ) {}
}