package com.example.sb10_MoPl_team3.sportsdb;

public final class SportsDbConstants {

  public static final String SOURCE_SPORTS_DB = "SPORTS_DB";

  private SportsDbConstants() {
  }

  public static String externalId(String eventId) {
    return "EVENT-" + eventId;
  }
}