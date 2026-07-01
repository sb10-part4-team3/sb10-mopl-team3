package com.example.sb10_MoPl_team3.watchingsession.service;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;
import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionJoinResult;
import com.example.sb10_MoPl_team3.watchingsession.repository.WatchingSessionRedisRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class WatchingSessionPresenceServiceTest {

    @Mock WatchingSessionPersistenceService persistenceService;
    @Mock WatchingSessionRedisRepository redisRepository;
    @InjectMocks WatchingSessionPresenceService presenceService;

    @Test
    @DisplayName("콘텐츠 방 입장 시 시청 세션을 저장하고 현재 시청자 명단을 반환한다")
    void join_createsSessionAndReturnsWatchers() {
        UUID contentId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID watcherId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        User watcher = user(watcherId, "시청자");
        UserSummary summary = summary(watcher);
        given(persistenceService.join(contentId, watcherId))
                .willReturn(new WatchingSessionJoinResult(Optional.empty(), summary));
        given(redisRepository.addWatcher(contentId, summary)).willReturn(true);
        given(redisRepository.findWatchers(contentId)).willReturn(List.of(summary));

        var changes = presenceService.join(contentId, watcherId);

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).contentId()).isEqualTo(contentId);
        assertThat(changes.get(0).watchers()).extracting(w -> w.userId())
                .containsExactly(watcherId);
        then(persistenceService).should().join(contentId, watcherId);
        then(redisRepository).should().addWatcher(contentId, summary);
    }

    @Test
    @DisplayName("콘텐츠 방 퇴장 시 시청 세션을 제거하고 빈 명단을 반환한다")
    void leave_removesSessionAndReturnsWatchers() {
        UUID contentId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID watcherId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        given(redisRepository.findWatchers(contentId)).willReturn(List.of());

        var change = presenceService.leave(contentId, watcherId);

        assertThat(change.watchers()).isEmpty();
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
    @DisplayName("이미 같은 콘텐츠를 시청 중이면 JPA 세션을 중복 저장하지 않는다")
    void join_sameContentDoesNotDuplicateSession() {
        UUID contentId = UUID.randomUUID();
        UUID watcherId = UUID.randomUUID();
        User watcher = user(watcherId, "시청자");
        UserSummary summary = summary(watcher);
        given(persistenceService.join(contentId, watcherId))
                .willReturn(new WatchingSessionJoinResult(Optional.empty(), summary));
        given(redisRepository.addWatcher(contentId, summary)).willReturn(false);

        var changes = presenceService.join(contentId, watcherId);

        assertThat(changes).isEmpty();
        then(persistenceService).should().join(contentId, watcherId);
        then(redisRepository).should().addWatcher(contentId, summary);
        then(redisRepository).should(never()).findWatchers(contentId);
    }

    @Test
    @DisplayName("다른 콘텐츠로 이동하면 이전 방과 새 방 명단을 모두 반환한다")
    void join_movesBetweenContents() {
        UUID previousId = UUID.randomUUID();
        UUID nextId = UUID.randomUUID();
        UUID watcherId = UUID.randomUUID();
        User watcher = user(watcherId, "시청자");
        UserSummary summary = summary(watcher);
        given(persistenceService.join(nextId, watcherId))
                .willReturn(new WatchingSessionJoinResult(Optional.of(previousId), summary));
        given(redisRepository.addWatcher(nextId, summary)).willReturn(true);
        given(redisRepository.findWatchers(previousId)).willReturn(List.of());
        given(redisRepository.findWatchers(nextId)).willReturn(List.of(summary));

        var changes = presenceService.join(nextId, watcherId);

        assertThat(changes).extracting(change -> change.contentId())
                .containsExactly(previousId, nextId);
        then(persistenceService).should().join(nextId, watcherId);
        then(redisRepository).should().removeWatcher(previousId, watcherId);
    }

    @Test
    @DisplayName("시청 세션이 없는 사용자의 퇴장은 멱등적으로 처리한다")
    void leave_withoutSession() {
        UUID contentId = UUID.randomUUID();
        UUID watcherId = UUID.randomUUID();
        given(redisRepository.findWatchers(contentId)).willReturn(List.of());

        var change = presenceService.leave(contentId, watcherId);

        assertThat(change.watchers()).isEmpty();
        then(persistenceService).should().leave(contentId, watcherId);
        then(redisRepository).should().removeWatcher(contentId, watcherId);
    }

    private User user(UUID id, String name) {
        User user = new User(id + "@test.com", name, "password", null, UserRole.USER);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private UserSummary summary(User user) {
        return new UserSummary(user.getId(), user.getName(), user.getProfileImageUrl());
    }

    private Content content(UUID id) {
        Content content = Content.builder()
                .type(ContentType.MOVIE).title("콘텐츠").description("설명")
                .thumbnailUrl("thumbnail").externalId(id.toString()).source("test").build();
        ReflectionTestUtils.setField(content, "id", id);
        return content;
    }
}
