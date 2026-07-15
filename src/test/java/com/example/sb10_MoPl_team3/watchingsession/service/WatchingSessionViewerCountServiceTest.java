package com.example.sb10_MoPl_team3.watchingsession.service;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doAnswer;

@ExtendWith(MockitoExtension.class)
class WatchingSessionViewerCountServiceTest {

    @Mock WatchingSessionViewerCountLockManager lockManager;
    @Mock WatchingSessionViewerCountSyncWorker syncWorker;

    @Test
    void sync_runsWorkerWithContentLock() {
        var service = new WatchingSessionViewerCountService(lockManager, syncWorker);
        UUID contentId = UUID.randomUUID();
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(1).run();
            return true;
        }).when(lockManager).executeWithLock(any(UUID.class), any(Runnable.class));

        service.sync(contentId);

        then(lockManager).should().executeWithLock(any(UUID.class), any(Runnable.class));
        then(syncWorker).should().sync(contentId);
    }
}
