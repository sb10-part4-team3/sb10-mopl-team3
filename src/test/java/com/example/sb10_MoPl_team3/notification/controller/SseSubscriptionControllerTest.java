package com.example.sb10_MoPl_team3.notification.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.sb10_MoPl_team3.global.security.AuthUser;
import com.example.sb10_MoPl_team3.global.sse.SseConnectionRepository;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SseSubscriptionControllerTest {

    @Mock SseConnectionRepository connectionRepository;
    @InjectMocks SseSubscriptionController controller;

    @Test
    void subscribe_replaysFromStandardLastEventIdHeader() {
        UUID userId = UUID.randomUUID();
        AuthUser authUser = new AuthUser(userId, UserRole.USER, UUID.randomUUID());
        given(connectionRepository.findCachedEventsAfter(userId, "header-event"))
                .willReturn(List.of());

        controller.subscribe(authUser, "header-event", "query-event");

        then(connectionRepository).should().findCachedEventsAfter(userId, "header-event");
    }

    @Test
    void subscribe_supportsApiQueryParameterWhenHeaderIsMissing() {
        UUID userId = UUID.randomUUID();
        AuthUser authUser = new AuthUser(userId, UserRole.USER, UUID.randomUUID());
        given(connectionRepository.findCachedEventsAfter(userId, "query-event"))
                .willReturn(List.of());

        controller.subscribe(authUser, null, "query-event");

        then(connectionRepository).should().findCachedEventsAfter(userId, "query-event");
    }
}
