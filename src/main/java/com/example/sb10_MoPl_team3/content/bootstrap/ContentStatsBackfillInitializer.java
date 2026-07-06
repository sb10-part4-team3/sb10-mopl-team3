package com.example.sb10_MoPl_team3.content.bootstrap;

import com.example.sb10_MoPl_team3.content.service.ContentStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContentStatsBackfillInitializer implements ApplicationRunner {

  private final ContentStatsService contentStatsService;

  @Override
  public void run(ApplicationArguments args) {
    contentStatsService.backfillMissingStats();
  }
}
