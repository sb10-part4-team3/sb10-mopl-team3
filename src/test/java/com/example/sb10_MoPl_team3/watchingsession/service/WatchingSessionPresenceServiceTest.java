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
import com.example.sb10_MoPl_team3.watchingsession.repository.WatchingSessionRedisRepository;
import com.example.sb10_MoPl_team3.watchingsession.repository.WatchingSessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class WatchingSessionPresenceServiceTest {

    @Mock WatchingSessionRepository watchingSessionRepository;
    @Mock WatchingSessionRedisRepository redisRepository;
    @Mock UserRepository userRepository;
    @Mock ContentRepository contentRepository;
    @InjectMocks WatchingSessionPresenceService presenceService;

    @Test
    @DisplayName("콘텐츠 방 입장 시 시청 세션을 저장하고 현재 시청자 명단을 반환한다")
    void join_createsSessionAndReturnsWatchers() {
        UUID contentId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID watcherId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        User watcher = user(watcherId, "시청자");
        Content content = content(contentId);
        given(userRepository.findById(watcherId)).willReturn(Optional.of(watcher));
        given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
        given(watchingSessionRepository.findByWatcherId(watcherId)).willReturn(Optional.empty());
        given(redisRepository.findWatcherIds(contentId)).willReturn(Set.of(watcherId));
        given(userRepository.findAllById(Set.of(watcherId))).willReturn(List.of(watcher));

        var changes = presenceService.join(contentId, watcherId);

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).contentId()).isEqualTo(contentId);
        assertThat(changes.get(0).watchers()).extracting(w -> w.userId())
                .containsExactly(watcherId);
        then(watchingSessionRepository).should().save(any(WatchingSession.class));
        then(redisRepository).should().addWatcher(contentId, watcherId);
    }

    @Test
    @DisplayName("콘텐츠 방 퇴장 시 시청 세션을 제거하고 빈 명단을 반환한다")
    void leave_removesSessionAndReturnsWatchers() {
        UUID contentId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID watcherId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        WatchingSession session = new WatchingSession(user(watcherId, "시청자"), content(contentId));
        given(watchingSessionRepository.findByWatcherId(watcherId)).willReturn(Optional.of(session));
        given(redisRepository.findWatcherIds(contentId)).willReturn(Set.of());
        given(userRepository.findAllById(Set.of())).willReturn(List.of());

        var change = presenceService.leave(contentId, watcherId);

        assertThat(change.watchers()).isEmpty();
        then(watchingSessionRepository).should().delete(session);
        then(redisRepository).should().removeWatcher(contentId, watcherId);
    }

    @Test
    @DisplayName("존재하지 않는 사용자가 입장하면 USER_NOT_FOUND 예외를 던진다")
    void join_userNotFound() {
        UUID contentId = UUID.randomUUID();
        UUID watcherId = UUID.randomUUID();
        given(userRepository.findById(watcherId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> presenceService.join(contentId, watcherId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
        then(contentRepository).shouldHaveNoInteractions();
        then(redisRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("존재하지 않는 콘텐츠에 입장하면 CONTENT_NOT_FOUND 예외를 던진다")
    void join_contentNotFound() {
        UUID contentId = UUID.randomUUID();
        UUID watcherId = UUID.randomUUID();
        given(userRepository.findById(watcherId)).willReturn(Optional.of(user(watcherId, "시청자")));
        given(contentRepository.findById(contentId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> presenceService.join(contentId, watcherId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CONTENT_NOT_FOUND);
        then(watchingSessionRepository).shouldHaveNoInteractions();
        then(redisRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("이미 같은 콘텐츠를 시청 중이면 JPA 세션을 중복 저장하지 않는다")
    void join_sameContentDoesNotDuplicateSession() {
        UUID contentId = UUID.randomUUID();
        UUID watcherId = UUID.randomUUID();
        User watcher = user(watcherId, "시청자");
        Content content = content(contentId);
        given(userRepository.findById(watcherId)).willReturn(Optional.of(watcher));
        given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
        given(watchingSessionRepository.findByWatcherId(watcherId))
                .willReturn(Optional.of(new WatchingSession(watcher, content)));
        given(redisRepository.findWatcherIds(contentId)).willReturn(Set.of(watcherId));
        given(userRepository.findAllById(Set.of(watcherId))).willReturn(List.of(watcher));

        presenceService.join(contentId, watcherId);

        then(watchingSessionRepository).should(never()).save(any());
        then(watchingSessionRepository).should(never()).delete(any());
        then(redisRepository).should().addWatcher(contentId, watcherId);
    }

    @Test
    @DisplayName("다른 콘텐츠로 이동하면 이전 방과 새 방 명단을 모두 반환한다")
    void join_movesBetweenContents() {
        UUID previousId = UUID.randomUUID();
        UUID nextId = UUID.randomUUID();
        UUID watcherId = UUID.randomUUID();
        User watcher = user(watcherId, "시청자");
        WatchingSession previous = new WatchingSession(watcher, content(previousId));
        given(userRepository.findById(watcherId)).willReturn(Optional.of(watcher));
        given(contentRepository.findById(nextId)).willReturn(Optional.of(content(nextId)));
        given(watchingSessionRepository.findByWatcherId(watcherId)).willReturn(Optional.of(previous));
        given(redisRepository.findWatcherIds(previousId)).willReturn(Set.of());
        given(redisRepository.findWatcherIds(nextId)).willReturn(Set.of(watcherId));
        given(userRepository.findAllById(Set.of())).willReturn(List.of());
        given(userRepository.findAllById(Set.of(watcherId))).willReturn(List.of(watcher));

        var changes = presenceService.join(nextId, watcherId);

        assertThat(changes).extracting(change -> change.contentId())
                .containsExactly(previousId, nextId);
        then(watchingSessionRepository).should().delete(previous);
        then(watchingSessionRepository).should().flush();
        then(redisRepository).should().removeWatcher(previousId, watcherId);
        then(watchingSessionRepository).should().save(any(WatchingSession.class));
    }

    @Test
    @DisplayName("시청 세션이 없는 사용자의 퇴장은 멱등적으로 처리한다")
    void leave_withoutSession() {
        UUID contentId = UUID.randomUUID();
        UUID watcherId = UUID.randomUUID();
        given(watchingSessionRepository.findByWatcherId(watcherId)).willReturn(Optional.empty());
        given(redisRepository.findWatcherIds(contentId)).willReturn(Set.of());
        given(userRepository.findAllById(Set.of())).willReturn(List.of());

        var change = presenceService.leave(contentId, watcherId);

        assertThat(change.watchers()).isEmpty();
        then(watchingSessionRepository).should(never()).delete(any());
        then(redisRepository).should().removeWatcher(contentId, watcherId);
    }

    private User user(UUID id, String name) {
        User user = new User(id + "@test.com", name, "password", null, UserRole.USER);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Content content(UUID id) {
        Content content = Content.builder()
                .type(ContentType.MOVIE).title("콘텐츠").description("설명")
                .thumbnailUrl("thumbnail").externalId(id.toString()).source("test").build();
        ReflectionTestUtils.setField(content, "id", id);
        return content;
    }
}
