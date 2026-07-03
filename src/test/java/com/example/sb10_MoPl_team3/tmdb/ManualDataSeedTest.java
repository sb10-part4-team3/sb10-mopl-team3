package com.example.sb10_MoPl_team3.tmdb;

import com.example.sb10_MoPl_team3.sportsdb.service.SportsDbContentSyncService;
import com.example.sb10_MoPl_team3.tmdb.service.TmdbContentSyncService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;

@SpringBootTest
@Tag("external")
class ManualDataSeedTest {

  @Autowired
  private TmdbContentSyncService tmdbContentSyncService;

  @Autowired
  private SportsDbContentSyncService sportsDbContentSyncService;

  @Test
  @Commit
  void 실제_DB에_콘텐츠_채우기() {
    tmdbContentSyncService.syncPopularMovies(1);
    tmdbContentSyncService.syncPopularTvs(1);
    sportsDbContentSyncService.syncNextEvents();
    sportsDbContentSyncService.syncPastEvents();
  }
}