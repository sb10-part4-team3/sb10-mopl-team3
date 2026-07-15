package com.example.sb10_MoPl_team3.watchingsession.websocket;

import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionChange;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WatchingSessionLocalEventDispatcher {

    private static final String WATCH_DESTINATION_FORMAT = "/sub/contents/%s/watch";

    private final SimpMessagingTemplate messagingTemplate;

    public void dispatch(WatchingSessionChange change, String trigger) {
        UUID contentId = change.watchingSession().content().id();
        String destination = WATCH_DESTINATION_FORMAT.formatted(contentId);
        try {
            messagingTemplate.convertAndSend(destination, change);
        } catch (RuntimeException exception) {
            log.warn(
                    "시청 세션 변경 메시지 전송에 실패했습니다. trigger={}, destination={}, type={}, contentId={}, watcherId={}",
                    trigger,
                    destination,
                    change.type(),
                    contentId,
                    change.watchingSession().watcher().userId(),
                    exception
            );
        }
    }
}
