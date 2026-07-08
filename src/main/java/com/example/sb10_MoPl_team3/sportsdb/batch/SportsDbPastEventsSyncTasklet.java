package com.example.sb10_MoPl_team3.sportsdb.batch;

import com.example.sb10_MoPl_team3.sportsdb.service.SportsDbContentSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SportsDbPastEventsSyncTasklet implements Tasklet {

  private final SportsDbContentSyncService sportsDbContentSyncService;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    sportsDbContentSyncService.syncPastEvents();
    return RepeatStatus.FINISHED;
  }
}
