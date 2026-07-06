package com.example.sb10_MoPl_team3.content.service;

import java.util.UUID;

public interface ContentStatsService {
  void recalculate(UUID contentId);

  void backfillMissingStats();

}
