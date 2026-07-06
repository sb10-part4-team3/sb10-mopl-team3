package com.example.sb10_MoPl_team3.content.bootstrap;

import static org.mockito.BDDMockito.then;

import com.example.sb10_MoPl_team3.content.service.ContentStatsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

@ExtendWith(MockitoExtension.class)
class ContentStatsBackfillInitializerTest {

    @Mock
    private ContentStatsService contentStatsService;

    @Test
    @DisplayName("애플리케이션 시작 시 누락된 ContentStats를 백필한다")
    void run_backfillMissingStats호출() throws Exception {
        ContentStatsBackfillInitializer initializer = new ContentStatsBackfillInitializer(contentStatsService);

        initializer.run(new DefaultApplicationArguments());

        then(contentStatsService).should().backfillMissingStats();
    }
}
