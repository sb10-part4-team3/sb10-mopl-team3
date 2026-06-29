package com.example.sb10_MoPl_team3.watchingsession.service;

import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionDto;

import java.util.UUID;

public interface WatchingSessionService {

    WatchingSessionDto findByWatcher(UUID watcherId);
}
