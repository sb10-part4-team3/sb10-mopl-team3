package com.example.sb10_MoPl_team3.watchingsession.service;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.repository.ContentStatsRepository;
import com.example.sb10_MoPl_team3.content.repository.ContentTagRepository;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import com.example.sb10_MoPl_team3.watchingsession.entity.WatchingSession;
import com.example.sb10_MoPl_team3.watchingsession.repository.WatchingSessionRepository;
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
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class WatchingSessionServiceImplTest {

    @Mock
    private WatchingSessionRepository watchingSessionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContentStatsRepository contentStatsRepository;

    @Mock
    private ContentTagRepository contentTagRepository;

    @InjectMocks
    private WatchingSessionServiceImpl watchingSessionService;

    @Test
    @DisplayName("특정 사용자의 시청 세션을 WatchingSessionDto로 반환한다")
    void findByWatcher_success() {
        UUID watcherId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID contentId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID sessionId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        Instant createdAt = Instant.parse("2026-06-30T00:00:00Z");
        User watcher = user(watcherId);
        Content content = content(contentId);
        WatchingSession watchingSession = new WatchingSession(watcher, content);
        ReflectionTestUtils.setField(watchingSession, "id", sessionId);
        ReflectionTestUtils.setField(watchingSession, "createdAt", createdAt);

        given(userRepository.existsById(watcherId)).willReturn(true);
        given(watchingSessionRepository.findByWatcherId(watcherId))
                .willReturn(Optional.of(watchingSession));
        given(contentStatsRepository.findById(contentId)).willReturn(Optional.empty());
        given(contentTagRepository.findTagNamesByContentId(contentId))
                .willReturn(List.of("액션"));

        var result = watchingSessionService.findByWatcher(watcherId);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(sessionId);
        assertThat(result.createdAt()).isEqualTo(createdAt);
        assertThat(result.watcher().userId()).isEqualTo(watcherId);
        assertThat(result.content().id()).isEqualTo(contentId);
        assertThat(result.content().tags()).containsExactly("액션");
        assertThat(result.content().averageRating()).isZero();
        assertThat(result.content().reviewCount()).isZero();
    }

    @Test
    @DisplayName("특정 사용자가 시청 중이 아니면 null을 반환한다")
    void findByWatcher_notWatching() {
        UUID watcherId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        given(userRepository.existsById(watcherId)).willReturn(true);
        given(watchingSessionRepository.findByWatcherId(watcherId)).willReturn(Optional.empty());

        var result = watchingSessionService.findByWatcher(watcherId);

        assertThat(result).isNull();
        then(contentStatsRepository).shouldHaveNoInteractions();
        then(contentTagRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("조회 대상 사용자가 없으면 USER_NOT_FOUND 예외를 던진다")
    void findByWatcher_userNotFound() {
        UUID watcherId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        given(userRepository.existsById(watcherId)).willReturn(false);

        assertThatThrownBy(() -> watchingSessionService.findByWatcher(watcherId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
        then(watchingSessionRepository).should(never()).findByWatcherId(watcherId);
    }

    private User user(UUID id) {
        User user = new User(
                "watcher@example.com",
                "시청자",
                "password",
                null,
                UserRole.USER
        );
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Content content(UUID id) {
        Content content = Content.builder()
                .type(ContentType.MOVIE)
                .title("테스트 콘텐츠")
                .description("설명")
                .thumbnailUrl("https://example.com/thumbnail.jpg")
                .externalId("external-id")
                .source("test")
                .build();
        ReflectionTestUtils.setField(content, "id", id);
        return content;
    }
}
