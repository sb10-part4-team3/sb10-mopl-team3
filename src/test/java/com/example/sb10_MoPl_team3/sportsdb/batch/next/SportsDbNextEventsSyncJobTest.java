package com.example.sb10_MoPl_team3.sportsdb.batch.next;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.sb10_MoPl_team3.sportsdb.client.SportsDbApiClient;
import com.example.sb10_MoPl_team3.sportsdb.config.SportsDbProperties;
import com.example.sb10_MoPl_team3.sportsdb.dto.SportsDbEventsResponse;
import com.example.sb10_MoPl_team3.sportsdb.dto.SportsDbEventsResponse.SportsDbEvent;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
class SportsDbNextEventsSyncJobTest {

  private static final String LEAGUE_ID = "4328";

  @Autowired
  private JobLauncher jobLauncher;

  @Autowired
  @Qualifier("sportsDbNextEventsSyncJob")
  private Job sportsDbNextEventsSyncJob;

  @MockitoBean
  private SportsDbApiClient sportsDbApiClient;

  @MockitoBean
  private SportsDbProperties sportsDbProperties;

  @Test
  void sportsDbNextEventsSyncJob_실행하면_COMPLETED_상태로_종료된다() throws Exception {
    // given
    given(sportsDbProperties.getTargetLeagueIds()).willReturn(List.of(LEAGUE_ID));
    given(sportsDbApiClient.getNextEventsByLeague(LEAGUE_ID)).willReturn(sampleEventsResponse());

    JobParameters params = new JobParametersBuilder()
        .addLong("time", System.currentTimeMillis())
        .toJobParameters();

    // when
    JobExecution execution = jobLauncher.run(sportsDbNextEventsSyncJob, params);

    // then
    assertThat(execution.getStatus().toString()).isEqualTo("COMPLETED");
    then(sportsDbApiClient).should().getNextEventsByLeague(LEAGUE_ID);
  }

  private SportsDbEventsResponse sampleEventsResponse() {
    SportsDbEvent event = new SportsDbEvent(
        "1", "샘플 경기", "2026-08-01", "샘플 리그", "샘플 경기장", "https://sample.jpg");
    return new SportsDbEventsResponse(List.of(event));
  }
}
