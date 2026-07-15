package com.example.sb10_MoPl_team3.watchingsession.service;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.dto.ContentSummary;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;
import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionChangeType;
import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionDto;
import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionJoinResult;
import com.example.sb10_MoPl_team3.watchingsession.repository.WatchingSessionRedisRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class WatchingSessionPresenceServiceTest {

    @Mock WatchingSessionPersistenceService persistenceService;
    @Mock WatchingSessionRedisRepository redisRepository;
    @Mock WatchingSessionViewerCountService viewerCountService;
    @InjectMocks WatchingSessionPresenceService presenceService;

    @Test
    @DisplayName("콘텐츠 방 입장 시 시청 세션을 저장하고 현재 시청자 명단을 반환한다")
    void join_createsSessionAndReturnsWatchers() {
        UUID contentId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID watcherId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        User watcher = user(watcherId, "시청자");
        UserSummary summary = summary(watcher);
        WatchingSessionDto session = session(UUID.randomUUID(), contentId, summary);
        given(persistenceService.join(contentId, watcherId))
                .willReturn(new WatchingSessionJoinResult(Optional.empty(), session));
        given(redisRepository.addWatcher(contentId, summary)).willReturn(true);
        given(redisRepository.countWatchers(contentId)).willReturn(1L);

        var changes = presenceService.join(contentId, watcherId);

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).type()).isEqualTo(WatchingSessionChangeType.JOIN);
        assertThat(changes.get(0).watchingSession()).isEqualTo(session);
        assertThat(changes.get(0).watcherCount()).isEqualTo(1);
        then(persistenceService).should().join(contentId, watcherId);
        then(redisRepository).should().addWatcher(contentId, summary);
    }

    @Test
    @DisplayName("콘텐츠 방 퇴장 시 시청 세션을 제거하고 빈 명단을 반환한다")
    void leave_removesSessionAndReturnsWatchers() {
        UUID contentId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID watcherId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UserSummary summary = new UserSummary(watcherId, "시청자", null);
        WatchingSessionDto session = session(UUID.randomUUID(), contentId, summary);
        given(persistenceService.leave(contentId, watcherId)).willReturn(Optional.of(session));
        given(redisRepository.countWatchers(contentId)).willReturn(0L);

        var changes = presenceService.leave(contentId, watcherId);

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).type()).isEqualTo(WatchingSessionChangeType.LEAVE);
        assertThat(changes.get(0).watchingSession()).isEqualTo(session);
        assertThat(changes.get(0).watcherCount()).isZero();
        then(persistenceService).should().leave(contentId, watcherId);
        then(redisRepository).should().removeWatcher(contentId, watcherId);
    }

    @Test
    @DisplayName("존재하지 않는 사용자가 입장하면 USER_NOT_FOUND 예외를 던진다")
    void join_userNotFound() {
        UUID contentId = UUID.randomUUID();
        UUID watcherId = UUID.randomUUID();
        given(persistenceService.join(contentId, watcherId))
                .willThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

        assertThatThrownBy(() -> presenceService.join(contentId, watcherId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
        then(redisRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("존재하지 않는 콘텐츠에 입장하면 CONTENT_NOT_FOUND 예외를 던진다")
    void join_contentNotFound() {
        UUID contentId = UUID.randomUUID();
        UUID watcherId = UUID.randomUUID();
        given(persistenceService.join(contentId, watcherId))
                .willThrow(new BusinessException(ErrorCode.CONTENT_NOT_FOUND));

        assertThatThrownBy(() -> presenceService.join(contentId, watcherId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CONTENT_NOT_FOUND);
        then(redisRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("이미 같은 콘텐츠를 시청 중이면 세션을 중복 저장하지 않고 현재 명단을 반환한다")
    void join_sameContentDoesNotDuplicateSession() {
        UUID contentId = UUID.randomUUID();
        UUID watcherId = UUID.randomUUID();
        User watcher = user(watcherId, "시청자");
        UserSummary summary = summary(watcher);
        WatchingSessionDto session = session(UUID.randomUUID(), contentId, summary);
        given(persistenceService.join(contentId, watcherId))
                .willReturn(new WatchingSessionJoinResult(Optional.empty(), session));
        given(redisRepository.addWatcher(contentId, summary)).willReturn(false);
        given(redisRepository.countWatchers(contentId)).willReturn(1L);

        var changes = presenceService.join(contentId, watcherId);

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).type()).isEqualTo(WatchingSessionChangeType.JOIN);
        assertThat(changes.get(0).watchingSession()).isEqualTo(session);
        assertThat(changes.get(0).watcherCount()).isEqualTo(1);
        then(persistenceService).should().join(contentId, watcherId);
        then(redisRepository).should().addWatcher(contentId, summary);
        then(redisRepository).should().countWatchers(contentId);
    }

    @Test
    @DisplayName("다른 콘텐츠로 이동하면 이전 방과 새 방 명단을 모두 반환한다")
    void join_movesBetweenContents() {
        UUID previousId = UUID.randomUUID();
        UUID nextId = UUID.randomUUID();
        UUID watcherId = UUID.randomUUID();
        User watcher = user(watcherId, "시청자");
        UserSummary summary = summary(watcher);
        WatchingSessionDto previousSession = session(UUID.randomUUID(), previousId, summary);
        WatchingSessionDto nextSession = session(UUID.randomUUID(), nextId, summary);
        given(persistenceService.join(nextId, watcherId))
                .willReturn(new WatchingSessionJoinResult(Optional.of(previousSession), nextSession));
        given(redisRepository.addWatcher(nextId, summary)).willReturn(true);
        given(redisRepository.countWatchers(previousId)).willReturn(0L);
        given(redisRepository.countWatchers(nextId)).willReturn(1L);

        var changes = presenceService.join(nextId, watcherId);

        assertThat(changes).extracting(change -> change.type())
                .containsExactly(WatchingSessionChangeType.LEAVE, WatchingSessionChangeType.JOIN);
        assertThat(changes).extracting(change -> change.watchingSession())
                .containsExactly(previousSession, nextSession);
        then(persistenceService).should().join(nextId, watcherId);
        then(redisRepository).should().removeWatcher(previousId, watcherId);
    }

    @Test
    @DisplayName("시청 세션이 없는 사용자의 퇴장은 멱등적으로 처리한다")
    void leave_withoutSession() {
        UUID contentId = UUID.randomUUID();
        UUID watcherId = UUID.randomUUID();
        given(persistenceService.leave(contentId, watcherId)).willReturn(Optional.empty());

        var changes = presenceService.leave(contentId, watcherId);

        assertThat(changes).isEmpty();
        then(persistenceService).should().leave(contentId, watcherId);
        then(redisRepository).should().removeWatcher(contentId, watcherId);
    }

    @Test
    @DisplayName("만료된 시청자를 정리할 때 현재 시청자 수는 한 번만 조회해 재사용한다")
    void removeStaleWatchers_reusesCurrentWatcherCount() {
        UUID contentId = UUID.randomUUID();
        UUID firstWatcherId = UUID.randomUUID();
        UUID secondWatcherId = UUID.randomUUID();
        UserSummary first = new UserSummary(firstWatcherId, "첫번째", null);
        UserSummary second = new UserSummary(secondWatcherId, "두번째", null);
        WatchingSessionDto firstSession = session(UUID.randomUUID(), contentId, first);
        WatchingSessionDto secondSession = session(UUID.randomUUID(), contentId, second);
        given(redisRepository.removeStaleWatchers(contentId)).willReturn(List.of(first, second));
        given(redisRepository.countWatchers(contentId)).willReturn(1L);
        given(persistenceService.leave(contentId, firstWatcherId)).willReturn(Optional.of(firstSession));
        given(persistenceService.leave(contentId, secondWatcherId)).willReturn(Optional.of(secondSession));

        var changes = presenceService.removeStaleWatchers(contentId);

        assertThat(changes).hasSize(2);
        assertThat(changes).extracting(change -> change.watcherCount())
                .containsExactly(1L, 1L);
        then(redisRepository).should().countWatchers(contentId);
        then(viewerCountService).should().sync(contentId);
    }

    private User user(UUID id, String name) {
        User user = new User(id + "@test.com", name, "password", null, UserRole.USER);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private UserSummary summary(User user) {
        return new UserSummary(user.getId(), user.getName(), user.getProfileImageUrl());
    }

    private WatchingSessionDto session(UUID sessionId, UUID contentId, UserSummary watcher) {
        return new WatchingSessionDto(
                sessionId,
                Instant.now(),
                watcher,
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
        );
    }

}
