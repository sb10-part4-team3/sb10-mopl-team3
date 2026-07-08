package com.example.sb10_MoPl_team3.sportsdb.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

import com.example.sb10_MoPl_team3.sportsdb.service.SportsDbContentSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.repeat.RepeatStatus;

@ExtendWith(MockitoExtension.class)
class SportsDbPastEventsSyncTaskletTest {

  @Mock
  private SportsDbContentSyncService sportsDbContentSyncService;

  @InjectMocks
  private SportsDbPastEventsSyncTasklet sportsDbPastEventsSyncTasklet;

  @Test
  void execute_호출하면_syncPastEvents를_실행하고_FINISHED를_반환한다() {
    // when
    RepeatStatus result = sportsDbPastEventsSyncTasklet.execute(null, null);

    // then
    assertThat(result).isEqualTo(RepeatStatus.FINISHED);
    then(sportsDbContentSyncService).should().syncPastEvents();
  }
}
