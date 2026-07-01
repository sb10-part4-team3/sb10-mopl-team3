package com.example.sb10_MoPl_team3.watchingsession.service;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.repository.ContentRepository;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import com.example.sb10_MoPl_team3.watchingsession.entity.WatchingSession;
import com.example.sb10_MoPl_team3.watchingsession.repository.WatchingSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class WatchingSessionPersistenceServiceTest {

    @Mock WatchingSessionRepository watchingSessionRepository;
    @Mock UserRepository userRepository;
    @Mock ContentRepository contentRepository;
    @InjectMocks WatchingSessionPersistenceService persistenceService;

    @Test
    void join_createsNewSession() {
        UUID contentId = UUID.randomUUID();
        UUID watcherId = UUID.randomUUID();
        given(userRepository.findById(watcherId)).willReturn(Optional.of(user(watcherId)));
        given(contentRepository.findById(contentId)).willReturn(Optional.of(content(contentId)));
        given(watchingSessionRepository.findByWatcherId(watcherId)).willReturn(Optional.empty());

        var result = persistenceService.join(contentId, watcherId);
        assertThat(result.previousContentId()).isEmpty();
        assertThat(result.watcher().userId()).isEqualTo(watcherId);
        then(watchingSessionRepository).should().save(any(WatchingSession.class));
    }

    @Test
    void join_movesSessionAndReturnsPreviousContentId() {
        UUID previousId = UUID.randomUUID();
        UUID nextId = UUID.randomUUID();
        UUID watcherId = UUID.randomUUID();
        User watcher = user(watcherId);
        WatchingSession previous = new WatchingSession(watcher, content(previousId));
        given(userRepository.findById(watcherId)).willReturn(Optional.of(watcher));
        given(contentRepository.findById(nextId)).willReturn(Optional.of(content(nextId)));
        given(watchingSessionRepository.findByWatcherId(watcherId)).willReturn(Optional.of(previous));

        assertThat(persistenceService.join(nextId, watcherId).previousContentId())
                .contains(previousId);
        then(watchingSessionRepository).should().delete(previous);
        then(watchingSessionRepository).should().flush();
        then(watchingSessionRepository).should().save(any(WatchingSession.class));
    }

    @Test
    void join_sameContentDoesNotWrite() {
        UUID contentId = UUID.randomUUID();
        UUID watcherId = UUID.randomUUID();
        User watcher = user(watcherId);
        given(userRepository.findById(watcherId)).willReturn(Optional.of(watcher));
        given(contentRepository.findById(contentId)).willReturn(Optional.of(content(contentId)));
        given(watchingSessionRepository.findByWatcherId(watcherId))
                .willReturn(Optional.of(new WatchingSession(watcher, content(contentId))));

        assertThat(persistenceService.join(contentId, watcherId).previousContentId()).isEmpty();
        then(watchingSessionRepository).should(never()).save(any());
        then(watchingSessionRepository).should(never()).delete(any());
    }

    @Test
    void join_userNotFound() {
        UUID watcherId = UUID.randomUUID();
        given(userRepository.findById(watcherId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> persistenceService.join(UUID.randomUUID(), watcherId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void join_contentNotFound() {
        UUID contentId = UUID.randomUUID();
        UUID watcherId = UUID.randomUUID();
        given(userRepository.findById(watcherId)).willReturn(Optional.of(user(watcherId)));
        given(contentRepository.findById(contentId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> persistenceService.join(contentId, watcherId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CONTENT_NOT_FOUND);
    }

    @Test
    void leave_deletesMatchingSession() {
        UUID contentId = UUID.randomUUID();
        UUID watcherId = UUID.randomUUID();
        WatchingSession session = new WatchingSession(user(watcherId), content(contentId));
        given(watchingSessionRepository.findByWatcherId(watcherId)).willReturn(Optional.of(session));

        persistenceService.leave(contentId, watcherId);

        then(watchingSessionRepository).should().delete(session);
    }

    @Test
    void leave_doesNotDeleteSessionForDifferentContent() {
        UUID watchingContentId = UUID.randomUUID();
        UUID requestedContentId = UUID.randomUUID();
        UUID watcherId = UUID.randomUUID();
        WatchingSession session = new WatchingSession(
                user(watcherId), content(watchingContentId));
        given(watchingSessionRepository.findByWatcherId(watcherId))
                .willReturn(Optional.of(session));

        persistenceService.leave(requestedContentId, watcherId);

        then(watchingSessionRepository).should(never()).delete(any());
    }

    private User user(UUID id) {
        User user = new User(id + "@test.com", "시청자", "password", null, UserRole.USER);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Content content(UUID id) {
        Content content = Content.builder().type(ContentType.MOVIE).title("콘텐츠")
                .description("설명").thumbnailUrl("thumbnail")
                .externalId(id.toString()).source("test").build();
        ReflectionTestUtils.setField(content, "id", id);
        return content;
    }
}
