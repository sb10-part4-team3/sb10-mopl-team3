package com.example.sb10_MoPl_team3.watchingsession.websocket;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.dto.ContentSummary;
import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;
import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionChange;
import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionChangeType;
import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class WatchingSessionBroadcastSubscriberTest {

    @Mock WatchingSessionLocalEventDispatcher localEventDispatcher;
    @Mock Message message;

    @Test
    void onMessage_dispatchesChangeToLocalSubscribers() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        WatchingSessionBroadcastSubscriber subscriber = new WatchingSessionBroadcastSubscriber(
                objectMapper,
                localEventDispatcher);
        WatchingSessionChange change = change(UUID.randomUUID(), UUID.randomUUID());
        given(message.getBody()).willReturn(objectMapper.writeValueAsString(change)
                .getBytes(StandardCharsets.UTF_8));

        subscriber.onMessage(message, null);

        then(localEventDispatcher).should().dispatch(eq(change), eq("redis-broadcast"));
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
