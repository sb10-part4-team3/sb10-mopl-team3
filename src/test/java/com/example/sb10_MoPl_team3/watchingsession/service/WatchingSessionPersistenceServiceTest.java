package com.example.sb10_MoPl_team3.watchingsession.service;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.dto.ContentSummary;
import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.entity.ContentStats;
import com.example.sb10_MoPl_team3.content.mapper.ContentMapper;
import com.example.sb10_MoPl_team3.content.repository.ContentRepository;
import com.example.sb10_MoPl_team3.content.repository.ContentStatsRepository;
import com.example.sb10_MoPl_team3.content.repository.ContentTagRepository;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.notification.enums.NotificationLevel;
import com.example.sb10_MoPl_team3.notification.event.NotificationAudienceType;
import com.example.sb10_MoPl_team3.notification.event.NotificationFanoutEvent;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.mapper.UserMapper;
import com.example.sb10_MoPl_team3.user.mapper.UserResponseMapper;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import com.example.sb10_MoPl_team3.watchingsession.entity.WatchingSession;
import com.example.sb10_MoPl_team3.watchingsession.event.WatchingSessionJoinedEvent;
import com.example.sb10_MoPl_team3.watchingsession.event.WatchingSessionLeftEvent;
import com.example.sb10_MoPl_team3.watchingsession.repository.WatchingSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class WatchingSessionPersistenceServiceTest {

    @Mock WatchingSessionRepository watchingSessionRepository;
    @Mock UserRepository userRepository;
    @Mock ContentRepository contentRepository;
    @Mock ContentStatsRepository contentStatsRepository;
    @Mock ContentTagRepository contentTagRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock UserResponseMapper userResponseMapper;
    @Mock ContentMapper contentMapper;
    @InjectMocks WatchingSessionPersistenceService persistenceService;

    @BeforeEach
    void setUp() {
        lenient().when(userResponseMapper.toSummary(any(User.class)))
                .thenAnswer(invocation -> UserMapper.toSummary(invocation.getArgument(0)));
        lenient().when(contentMapper.toSummary(any(Content.class), any(), any()))
                .thenAnswer(invocation -> contentSummary(
                        invocation.getArgument(0),
                        invocation.getArgument(1),
                        invocation.getArgument(2)
                ));
    }

    @Test
    void join_createsNewSession() {
        UUID contentId = UUID.randomUUID();
        UUID watcherId = UUID.randomUUID();
        given(userRepository.findById(watcherId)).willReturn(Optional.of(user(watcherId)));
        given(contentRepository.findById(contentId)).willReturn(Optional.of(content(contentId)));
        given(watchingSessionRepository.findByWatcherId(watcherId)).willReturn(Optional.empty());
        given(watchingSessionRepository.saveAndFlush(any(WatchingSession.class)))
                .willAnswer(invocation -> persisted(invocation.getArgument(0)));
        given(contentStatsRepository.findById(contentId)).willReturn(Optional.empty());
        given(contentTagRepository.findTagNamesByContentId(contentId)).willReturn(List.of());

        var result = persistenceService.join(contentId, watcherId);
        assertThat(result.previousWatchingSession()).isEmpty();
        assertThat(result.watchingSession().watcher().userId()).isEqualTo(watcherId);
        then(watchingSessionRepository).should().saveAndFlush(any(WatchingSession.class));

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        then(eventPublisher).should(org.mockito.Mockito.times(2)).publishEvent(eventCaptor.capture());
        List<Object> publishedEvents = eventCaptor.getAllValues();
        assertThat(publishedEvents).anySatisfy(published ->
                assertThat(published).isEqualTo(new WatchingSessionJoinedEvent(contentId)));

        NotificationFanoutEvent event = publishedEvents.stream()
                .filter(NotificationFanoutEvent.class::isInstance)
                .map(NotificationFanoutEvent.class::cast)
                .findFirst()
                .orElseThrow();
        assertThat(event.audienceType()).isEqualTo(NotificationAudienceType.FOLLOWERS);
        assertThat(event.sourceId()).isEqualTo(watcherId);
        assertThat(event.title()).isEqualTo("시청 시작");
        assertThat(event.content()).isEqualTo("시청자님이 '콘텐츠' 시청을 시작했습니다.");
        assertThat(event.level()).isEqualTo(NotificationLevel.INFO);
    }

    @Test
    void join_movesSessionAndReturnsPreviousContentId() {
        UUID previousId = UUID.randomUUID();
        UUID nextId = UUID.randomUUID();
        UUID watcherId = UUID.randomUUID();
        User watcher = user(watcherId);
        WatchingSession previous = new WatchingSession(watcher, content(previousId));
        persisted(previous);
        given(userRepository.findById(watcherId)).willReturn(Optional.of(watcher));
        given(contentRepository.findById(nextId)).willReturn(Optional.of(content(nextId)));
        given(watchingSessionRepository.findByWatcherId(watcherId)).willReturn(Optional.of(previous));
        given(watchingSessionRepository.saveAndFlush(any(WatchingSession.class)))
                .willAnswer(invocation -> persisted(invocation.getArgument(0)));
        given(contentStatsRepository.findById(previousId)).willReturn(Optional.empty());
        given(contentStatsRepository.findById(nextId)).willReturn(Optional.empty());
        given(contentTagRepository.findTagNamesByContentId(previousId)).willReturn(List.of());
        given(contentTagRepository.findTagNamesByContentId(nextId)).willReturn(List.of());

        assertThat(persistenceService.join(nextId, watcherId).previousWatchingSession())
                .get()
                .extracting(session -> session.content().id())
                .isEqualTo(previousId);
        then(watchingSessionRepository).should().delete(previous);
        then(watchingSessionRepository).should().flush();
        then(watchingSessionRepository).should().saveAndFlush(any(WatchingSession.class));

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        then(eventPublisher).should(org.mockito.Mockito.atLeastOnce()).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues())
                .contains(new WatchingSessionLeftEvent(previousId), new WatchingSessionJoinedEvent(nextId));
    }

    @Test
    void join_sameContentDoesNotWrite() {
        UUID contentId = UUID.randomUUID();
        UUID watcherId = UUID.randomUUID();
        User watcher = user(watcherId);
        given(userRepository.findById(watcherId)).willReturn(Optional.of(watcher));
        given(contentRepository.findById(contentId)).willReturn(Optional.of(content(contentId)));
        WatchingSession existing = new WatchingSession(watcher, content(contentId));
        persisted(existing);
        given(watchingSessionRepository.findByWatcherId(watcherId))
                .willReturn(Optional.of(existing));
        given(contentStatsRepository.findById(contentId)).willReturn(Optional.empty());
        given(contentTagRepository.findTagNamesByContentId(contentId)).willReturn(List.of());

        assertThat(persistenceService.join(contentId, watcherId).previousWatchingSession()).isEmpty();
        then(watchingSessionRepository).should(never()).saveAndFlush(any());
        then(watchingSessionRepository).should(never()).delete(any());
        then(eventPublisher).shouldHaveNoInteractions();
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
        persisted(session);
        given(watchingSessionRepository.findByWatcherId(watcherId)).willReturn(Optional.of(session));
        given(contentStatsRepository.findById(contentId)).willReturn(Optional.empty());
        given(contentTagRepository.findTagNamesByContentId(contentId)).willReturn(List.of());

        var result = persistenceService.leave(contentId, watcherId);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(session.getId());
        then(watchingSessionRepository).should().delete(session);
        then(eventPublisher).should().publishEvent(new WatchingSessionLeftEvent(contentId));
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
        then(contentStatsRepository).shouldHaveNoInteractions();
    }

    @Test
    void leave_missingSessionDoesNotDecrementViewerCount() {
        UUID contentId = UUID.randomUUID();
        UUID watcherId = UUID.randomUUID();
        given(watchingSessionRepository.findByWatcherId(watcherId)).willReturn(Optional.empty());

        assertThat(persistenceService.leave(contentId, watcherId)).isEmpty();

        then(watchingSessionRepository).should(never()).delete(any());
        then(contentStatsRepository).shouldHaveNoInteractions();
    }

    private WatchingSession persisted(WatchingSession session) {
        if (session.getId() == null) {
            ReflectionTestUtils.setField(session, "id", UUID.randomUUID());
        }
        if (session.getCreatedAt() == null) {
            ReflectionTestUtils.setField(session, "createdAt", Instant.now());
        }
        return session;
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

    private ContentSummary contentSummary(Content content, ContentStats stats, List<String> tags) {
        return new ContentSummary(
                content.getId(),
                content.getType(),
                content.getTitle(),
                content.getDescription(),
                content.getThumbnailUrl(),
                tags,
                stats != null && stats.getAverageRating() != null
                        ? stats.getAverageRating().doubleValue()
                        : 0.0,
                stats != null ? stats.getReviewCount() : 0
        );
    }
}
