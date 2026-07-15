package com.example.sb10_MoPl_team3.watchingsession.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WatchingSessionViewerCountService {

    private final WatchingSessionViewerCountLockManager lockManager;
    private final WatchingSessionViewerCountSyncWorker syncWorker;

    public void sync(UUID contentId) {
        lockManager.executeWithLock(contentId, () -> syncWorker.sync(contentId));
    }
}
