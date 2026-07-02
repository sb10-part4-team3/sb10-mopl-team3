package com.example.sb10_MoPl_team3.watchingsession.event;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;
import com.example.sb10_MoPl_team3.user.event.UserProfileUpdatedEvent;
import com.example.sb10_MoPl_team3.user.event.UserWithdrawnEvent;
import com.example.sb10_MoPl_team3.watchingsession.repository.WatchingSessionRedisRepository;
import com.example.sb10_MoPl_team3.watchingsession.repository.WatchingSessionRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserSummaryCacheEventListenerTest {

    @Mock
    private WatchingSessionRepository watchingSessionRepository;

    @Mock
    private WatchingSessionRedisRepository redisRepository;

    @InjectMocks
    private UserSummaryCacheEventListener listener;

    @Test
    @DisplayName("시청 중인 사용자의 프로필이 수정되면 Redis 요약 정보를 갱신한다")
    void update_activeWatcher() {
        UUID userId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        UserSummary summary = new UserSummary(userId, "변경된 이름", "new-profile");
        given(watchingSessionRepository.findContentIdByWatcherId(userId))
                .willReturn(Optional.of(contentId));

        listener.update(new UserProfileUpdatedEvent(summary));

        then(redisRepository).should().addWatcher(contentId, summary);
    }

    @Test
    @DisplayName("시청 중이 아닌 사용자의 프로필 수정은 Redis를 변경하지 않는다")
    void update_inactiveWatcher() {
        UUID userId = UUID.randomUUID();
        UserSummary summary = new UserSummary(userId, "변경된 이름", null);
        given(watchingSessionRepository.findContentIdByWatcherId(userId))
                .willReturn(Optional.empty());

        listener.update(new UserProfileUpdatedEvent(summary));

        then(redisRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("시청 중인 사용자가 탈퇴하면 Redis 시청자 목록에서 제거한다")
    void remove_activeWatcher() {
        UUID userId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        given(watchingSessionRepository.findContentIdByWatcherId(userId))
                .willReturn(Optional.of(contentId));

        listener.remove(new UserWithdrawnEvent(userId));

        then(redisRepository).should().removeWatcher(contentId, userId);
    }

    @Test
    @DisplayName("시청 중이 아닌 사용자의 탈퇴는 Redis를 변경하지 않는다")
    void remove_inactiveWatcher() {
        UUID userId = UUID.randomUUID();
        given(watchingSessionRepository.findContentIdByWatcherId(userId))
                .willReturn(Optional.empty());

        listener.remove(new UserWithdrawnEvent(userId));

        then(redisRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Redis 갱신 실패는 사용자 정보 변경 결과에 영향을 주지 않는다")
    void update_redisFailureIsContained() {
        UUID userId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        UserSummary summary = new UserSummary(userId, "변경된 이름", null);
        given(watchingSessionRepository.findContentIdByWatcherId(userId))
                .willReturn(Optional.of(contentId));
        willThrow(new RuntimeException("Redis 연결 실패"))
                .given(redisRepository).addWatcher(contentId, summary);

        listener.update(new UserProfileUpdatedEvent(summary));

        then(redisRepository).should().addWatcher(contentId, summary);
    }

    @Test
    @DisplayName("Redis 제거 실패는 사용자 탈퇴 결과에 영향을 주지 않는다")
    void remove_redisFailureIsContained() {
        UUID userId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        given(watchingSessionRepository.findContentIdByWatcherId(userId))
                .willReturn(Optional.of(contentId));
        willThrow(new RuntimeException("Redis 연결 실패"))
                .given(redisRepository).removeWatcher(contentId, userId);

        listener.remove(new UserWithdrawnEvent(userId));

        then(redisRepository).should().removeWatcher(contentId, userId);
    }
}
