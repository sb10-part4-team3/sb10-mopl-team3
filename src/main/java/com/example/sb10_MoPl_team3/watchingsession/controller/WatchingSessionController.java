package com.example.sb10_MoPl_team3.watchingsession.controller;

import com.example.sb10_MoPl_team3.watchingsession.dto.CursorResponseWatchingSessionDto;
import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionDto;
import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionFindAllRequest;
import com.example.sb10_MoPl_team3.watchingsession.service.WatchingSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class WatchingSessionController {

    private final WatchingSessionService watchingSessionService;

    @GetMapping("/users/{watcherId}/watching-sessions")
    public ResponseEntity<WatchingSessionDto> findWatchingSessionByWatcher(
            @PathVariable UUID watcherId
    ) {
        return ResponseEntity.ok(watchingSessionService.findByWatcher(watcherId));
    }

    @GetMapping("/contents/{contentId}/watching-sessions")
    public ResponseEntity<CursorResponseWatchingSessionDto> findWatchingSessionsByContent(
            @PathVariable UUID contentId,
            @RequestParam(required = false) String watcherNameLike,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) UUID idAfter,
            @RequestParam int limit,
            @RequestParam String sortDirection,
            @RequestParam String sortBy
    ) {
        WatchingSessionFindAllRequest request = new WatchingSessionFindAllRequest(
                contentId, watcherNameLike, cursor, idAfter, limit, sortDirection, sortBy
        );
        return ResponseEntity.ok(watchingSessionService.findByContent(request));
    }
}
