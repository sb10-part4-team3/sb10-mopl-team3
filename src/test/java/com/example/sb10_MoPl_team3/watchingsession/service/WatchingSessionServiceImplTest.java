package com.example.sb10_MoPl_team3.watchingsession.service;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.repository.ContentStatsRepository;
import com.example.sb10_MoPl_team3.content.repository.ContentTagRepository;
import com.example.sb10_MoPl_team3.content.repository.ContentRepository;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import com.example.sb10_MoPl_team3.watchingsession.entity.WatchingSession;
import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionFindAllRequest;
import com.example.sb10_MoPl_team3.watchingsession.repository.WatchingSessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Pageable;
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
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.argThat;

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

    @Mock
    private ContentRepository contentRepository;

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

    @Test
    @DisplayName("콘텐츠 시청 세션을 limit만큼 반환하고 마지막 항목으로 다음 커서를 만든다")
    void findByContent_successWithNextCursor() {
        UUID contentId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Content content = content(contentId);
        WatchingSession first = session(
                "00000000-0000-0000-0000-000000000011",
                "00000000-0000-0000-0000-000000000021",
                "2026-06-30T02:00:00Z", content);
        WatchingSession second = session(
                "00000000-0000-0000-0000-000000000012",
                "00000000-0000-0000-0000-000000000022",
                "2026-06-30T01:00:00Z", content);

        given(contentRepository.existsById(contentId)).willReturn(true);
        given(watchingSessionRepository.findByContentDesc(
                org.mockito.ArgumentMatchers.eq(contentId),
                org.mockito.ArgumentMatchers.eq("시청"),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                argThat((Pageable pageable) ->
                        pageable.getPageNumber() == 0 && pageable.getPageSize() == 2)))
                .willReturn(List.of(first, second));
        given(watchingSessionRepository.countByContent(contentId, "시청")).willReturn(2L);
        given(contentStatsRepository.findById(contentId)).willReturn(Optional.empty());
        given(contentTagRepository.findTagNamesByContentId(contentId)).willReturn(List.of());

        var request = new WatchingSessionFindAllRequest(
                contentId, " 시청 ", null, null, 1, "DESCENDING", "createdAt");
        var result = watchingSessionService.findByContent(request);

        assertThat(result.data()).hasSize(1);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo("2026-06-30T02:00:00Z");
        assertThat(result.nextIdAfter()).isEqualTo(first.getId());
        assertThat(result.totalCount()).isEqualTo(2L);
        assertThat(result.sortBy()).isEqualTo("createdAt");
        assertThat(result.sortDirection()).isEqualTo("DESCENDING");
    }

    @Test
    @DisplayName("콘텐츠 요약 정보는 페이지 항목 수와 관계없이 한 번만 조회한다")
    void findByContent_reusesContentSummary() {
        UUID contentId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Content content = content(contentId);
        WatchingSession first = session(
                "00000000-0000-0000-0000-000000000011",
                "00000000-0000-0000-0000-000000000021",
                "2026-06-30T02:00:00Z", content);
        WatchingSession second = session(
                "00000000-0000-0000-0000-000000000012",
                "00000000-0000-0000-0000-000000000022",
                "2026-06-30T01:00:00Z", content);
        given(contentRepository.existsById(contentId)).willReturn(true);
        given(watchingSessionRepository.findByContentDesc(
                org.mockito.ArgumentMatchers.eq(contentId),
                org.mockito.ArgumentMatchers.eq(""),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                argThat((Pageable pageable) ->
                        pageable.getPageNumber() == 0 && pageable.getPageSize() == 21)))
                .willReturn(List.of(first, second));
        given(watchingSessionRepository.countByContent(contentId, "")).willReturn(2L);
        given(contentStatsRepository.findById(contentId)).willReturn(Optional.empty());
        given(contentTagRepository.findTagNamesByContentId(contentId)).willReturn(List.of("액션"));

        var result = watchingSessionService.findByContent(new WatchingSessionFindAllRequest(
                contentId, null, null, null, 20, "DESCENDING", "createdAt"));

        assertThat(result.data()).hasSize(2);
        assertThat(result.data()).allSatisfy(dto ->
                assertThat(dto.content().tags()).containsExactly("액션"));
        then(contentStatsRepository).should(times(1)).findById(contentId);
        then(contentTagRepository).should(times(1)).findTagNamesByContentId(contentId);
    }

    @Test
    @DisplayName("오름차순 요청은 ASC 조회 메서드와 limit + 1 페이지 크기를 사용한다")
    void findByContent_ascending() {
        UUID contentId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID idAfter = UUID.fromString("00000000-0000-0000-0000-000000000010");
        Instant cursor = Instant.parse("2026-06-30T00:00:00Z");
        Content content = content(contentId);
        WatchingSession session = session(
                "00000000-0000-0000-0000-000000000011",
                "00000000-0000-0000-0000-000000000021",
                "2026-06-30T01:00:00Z", content);
        given(contentRepository.existsById(contentId)).willReturn(true);
        given(watchingSessionRepository.findByContentAsc(
                org.mockito.ArgumentMatchers.eq(contentId),
                org.mockito.ArgumentMatchers.eq(""),
                org.mockito.ArgumentMatchers.eq(cursor),
                org.mockito.ArgumentMatchers.eq(idAfter),
                argThat((Pageable pageable) ->
                        pageable.getPageNumber() == 0 && pageable.getPageSize() == 4)))
                .willReturn(List.of(session));
        given(watchingSessionRepository.countByContent(contentId, "")).willReturn(1L);
        given(contentStatsRepository.findById(contentId)).willReturn(Optional.empty());
        given(contentTagRepository.findTagNamesByContentId(contentId)).willReturn(List.of());

        var result = watchingSessionService.findByContent(new WatchingSessionFindAllRequest(
                contentId, null, cursor.toString(), idAfter, 3, "ASCENDING", "createdAt"));

        assertThat(result.data()).extracting(dto -> dto.id()).containsExactly(session.getId());
        assertThat(result.sortDirection()).isEqualTo("ASCENDING");
        assertThat(result.hasNext()).isFalse();
        then(watchingSessionRepository).should(never()).findByContentDesc(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("커서와 보조 커서 중 하나만 전달되면 INVALID_CURSOR 예외를 던진다")
    void findByContent_invalidCursorPair() {
        UUID contentId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        given(contentRepository.existsById(contentId)).willReturn(true);
        var request = new WatchingSessionFindAllRequest(
                contentId, null, "2026-06-30T00:00:00Z", null,
                20, "ASCENDING", "createdAt");

        assertThatThrownBy(() -> watchingSessionService.findByContent(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CURSOR);
    }

    @Test
    @DisplayName("조회 대상 콘텐츠가 없으면 CONTENT_NOT_FOUND 예외를 던진다")
    void findByContent_contentNotFound() {
        UUID contentId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        given(contentRepository.existsById(contentId)).willReturn(false);
        var request = new WatchingSessionFindAllRequest(
                contentId, null, null, null, 20, "DESCENDING", "createdAt");

        assertThatThrownBy(() -> watchingSessionService.findByContent(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CONTENT_NOT_FOUND);
        then(watchingSessionRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("지원하지 않는 정렬 기준이면 INVALID_INPUT_VALUE 예외를 던진다")
    void findByContent_invalidSortBy() {
        UUID contentId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        given(contentRepository.existsById(contentId)).willReturn(true);
        var request = new WatchingSessionFindAllRequest(
                contentId, null, null, null, 20, "DESCENDING", "watcherName");

        assertThatThrownBy(() -> watchingSessionService.findByContent(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("지원하지 않는 정렬 방향이면 INVALID_SORT_DIRECTION 예외를 던진다")
    void findByContent_invalidSortDirection() {
        UUID contentId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        given(contentRepository.existsById(contentId)).willReturn(true);
        var request = new WatchingSessionFindAllRequest(
                contentId, null, null, null, 20, "SIDEWAYS", "createdAt");

        assertThatThrownBy(() -> watchingSessionService.findByContent(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_SORT_DIRECTION);
    }

    @Test
    @DisplayName("커서가 Instant 형식이 아니면 INVALID_CURSOR 예외를 던진다")
    void findByContent_malformedCursor() {
        UUID contentId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID idAfter = UUID.fromString("00000000-0000-0000-0000-000000000011");
        given(contentRepository.existsById(contentId)).willReturn(true);
        var request = new WatchingSessionFindAllRequest(
                contentId, null, "invalid-cursor", idAfter,
                20, "DESCENDING", "createdAt");

        assertThatThrownBy(() -> watchingSessionService.findByContent(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CURSOR);
    }

    private WatchingSession session(String sessionId, String watcherId, String createdAt, Content content) {
        WatchingSession session = new WatchingSession(user(UUID.fromString(watcherId)), content);
        ReflectionTestUtils.setField(session, "id", UUID.fromString(sessionId));
        ReflectionTestUtils.setField(session, "createdAt", Instant.parse(createdAt));
        return session;
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
