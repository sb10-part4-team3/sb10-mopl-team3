package com.example.sb10_MoPl_team3.watchingsession.service;

import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionDto;
import org.springframework.lang.Nullable;

import java.util.UUID;

public interface WatchingSessionService {

    @Nullable
    WatchingSessionDto findByWatcher(UUID watcherId);
}
