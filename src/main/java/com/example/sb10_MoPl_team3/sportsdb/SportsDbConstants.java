package com.example.sb10_MoPl_team3.sportsdb;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

public final class SportsDbConstants {

  public static final String SOURCE_SPORTS_DB = "SPORTS_DB";

  private SportsDbConstants() {
  }

  public static String externalId(String eventId) {
    return "EVENT-" + eventId;
  }

  public static String buildDescription(String league, String dateEvent) {
    return league + " " + dateEvent;
  }

  public static Instant parseEventDate(String dateEvent) {
    if (dateEvent == null || dateEvent.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(dateEvent).atStartOfDay(ZoneOffset.UTC).toInstant();
    } catch (DateTimeParseException e) {
      return null;
    }
  }
}