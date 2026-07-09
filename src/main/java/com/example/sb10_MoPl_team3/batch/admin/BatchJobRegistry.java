package com.example.sb10_MoPl_team3.batch.admin;

import java.util.List;

public final class BatchJobRegistry {

  public static final List<String> JOB_NAMES = List.of(
      "tmdbMovieSyncJob",
      "tmdbTvSyncJob",
      "sportsDbNextEventsSyncJob",
      "sportsDbPastEventsSyncJob"
  );

  private BatchJobRegistry() {
  }
}
