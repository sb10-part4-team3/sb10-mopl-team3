package com.example.sb10_MoPl_team3.watchingsession.websocket;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.dto.ContentSummary;
import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;
import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionChange;
import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionChangeType;
import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionDto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class WatchingSessionLocalEventDispatcherTest {

    @Mock SimpMessagingTemplate messagingTemplate;
    @InjectMocks WatchingSessionLocalEventDispatcher dispatcher;

    @Test
    void dispatch_sendsChangeToContentWatchDestination() {
        UUID contentId = UUID.randomUUID();
        WatchingSessionChange change = change(contentId, UUID.randomUUID());

        dispatcher.dispatch(change, "test");

        then(messagingTemplate).should()
                .convertAndSend("/sub/contents/" + contentId + "/watch", change);
    }

    @Test
    void dispatch_swallowsMessagingException() {
        UUID contentId = UUID.randomUUID();
        WatchingSessionChange change = change(contentId, UUID.randomUUID());
        doThrow(new RuntimeException("boom"))
                .when(messagingTemplate)
                .convertAndSend("/sub/contents/" + contentId + "/watch", change);

        dispatcher.dispatch(change, "test");

        then(messagingTemplate).should()
                .convertAndSend("/sub/contents/" + contentId + "/watch", change);
    }

    private WatchingSessionChange change(UUID contentId, UUID watcherId) {
        return new WatchingSessionChange(
                WatchingSessionChangeType.JOIN,
                new WatchingSessionDto(
                        UUID.randomUUID(),
                        Instant.now(),
                        new UserSummary(watcherId, "시청자", null),
                        new ContentSummary(
                                contentId,
                                ContentType.MOVIE,
                                "콘텐츠",
                                "설명",
                                "thumbnail",
                                List.of(),
                                0.0,
                                0
                        )
                ),
                1
        );
    }
}
