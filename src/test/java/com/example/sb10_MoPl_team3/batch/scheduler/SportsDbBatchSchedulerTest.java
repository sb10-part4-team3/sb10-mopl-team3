package com.example.sb10_MoPl_team3.batch.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

class SportsDbBatchSchedulerTest {

  @Test
  void runSportsDbNextEventsSyncJob은_Asia_Seoul_타임존으로_스케줄된다() throws NoSuchMethodException {
    assertThat(zoneOf("runSportsDbNextEventsSyncJob")).isEqualTo("Asia/Seoul");
  }

  @Test
  void runSportsDbPastEventsSyncJob은_Asia_Seoul_타임존으로_스케줄된다() throws NoSuchMethodException {
    assertThat(zoneOf("runSportsDbPastEventsSyncJob")).isEqualTo("Asia/Seoul");
  }

  private String zoneOf(String methodName) throws NoSuchMethodException {
    Method method = SportsDbBatchScheduler.class.getDeclaredMethod(methodName);
    return method.getAnnotation(Scheduled.class).zone();
  }
}
