package com.example.sb10_MoPl_team3.batch.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

class TmdbBatchSchedulerTest {

  @Test
  void runTmdbMovieSyncJob은_Asia_Seoul_타임존으로_스케줄된다() throws NoSuchMethodException {
    assertThat(zoneOf("runTmdbMovieSyncJob")).isEqualTo("Asia/Seoul");
  }

  @Test
  void runTmdbTvSyncJob은_Asia_Seoul_타임존으로_스케줄된다() throws NoSuchMethodException {
    assertThat(zoneOf("runTmdbTvSyncJob")).isEqualTo("Asia/Seoul");
  }

  private String zoneOf(String methodName) throws NoSuchMethodException {
    Method method = TmdbBatchScheduler.class.getDeclaredMethod(methodName);
    return method.getAnnotation(Scheduled.class).zone();
  }
}
