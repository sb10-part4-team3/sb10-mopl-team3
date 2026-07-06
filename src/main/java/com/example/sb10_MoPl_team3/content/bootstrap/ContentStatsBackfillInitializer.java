package com.example.sb10_MoPl_team3.content.bootstrap;

import com.example.sb10_MoPl_team3.content.service.ContentStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContentStatsBackfillInitializer implements ApplicationRunner {

  private final ContentStatsService contentStatsService;

  @Override
  public void run(ApplicationArguments args) {
    try {
      contentStatsService.backfillMissingStats();
    } catch (Exception e) {
      log.error("ContentStats 백필 중 오류가 발생했습니다. 애플리케이션 기동은 계속 진행합니다.", e);
    }
  }
}
