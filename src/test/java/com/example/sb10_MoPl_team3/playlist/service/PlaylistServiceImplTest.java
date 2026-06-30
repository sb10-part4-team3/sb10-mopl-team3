package com.example.sb10_MoPl_team3.playlist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.dto.ContentSummary;
import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.entity.ContentStats;
import com.example.sb10_MoPl_team3.content.repository.ContentRepository;
import com.example.sb10_MoPl_team3.content.repository.ContentStatsRepository;
import com.example.sb10_MoPl_team3.content.repository.ContentTagProjection;
import com.example.sb10_MoPl_team3.content.repository.ContentTagRepository;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.global.security.AuthUser;
import com.example.sb10_MoPl_team3.playlist.dto.request.PlaylistCreateRequest;
import com.example.sb10_MoPl_team3.playlist.dto.request.PlaylistFindAllRequest;
import com.example.sb10_MoPl_team3.playlist.dto.request.PlaylistUpdateRequest;
import com.example.sb10_MoPl_team3.playlist.dto.response.PlaylistDto;
import com.example.sb10_MoPl_team3.playlist.entity.Playlist;
import com.example.sb10_MoPl_team3.playlist.entity.PlaylistContent;
import com.example.sb10_MoPl_team3.playlist.enums.PlaylistStatus;
import com.example.sb10_MoPl_team3.playlist.exception.PlaylistNotFoundException;
import com.example.sb10_MoPl_team3.playlist.exception.PlaylistOwnerMismatchException;
import com.example.sb10_MoPl_team3.playlist.mapper.PlaylistMapper;
import com.example.sb10_MoPl_team3.playlist.repository.PlaylistContentRepository;
import com.example.sb10_MoPl_team3.playlist.repository.PlaylistRepository;
import com.example.sb10_MoPl_team3.playlist.repository.PlaylistRepositoryCustom;
import com.example.sb10_MoPl_team3.playlist.repository.PlaylistSubscriptionRepository;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PlaylistServiceImplTest {

    @Mock
    private PlaylistRepository playlistRepository;

    @Mock
    private PlaylistSubscriptionRepository playlistSubscriptionRepository;

    @Mock
    private PlaylistMapper playlistMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PlaylistContentRepository playlistContentRepository;

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private ContentTagRepository contentTagRepository;

    @Mock
    private ContentStatsRepository contentStatsRepository;

    @InjectMocks
    private PlaylistServiceImpl playlistService;

    @AfterEach
    void tearDown() {
        // SecurityUtils가 정적 SecurityContextHolder를 읽기 때문에 테스트 간 인증 상태를 격리한다.
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("create saves active playlist by authenticated user")
    void create_success() {
        UUID userId = uuid(1);
        User owner = user(userId, "owner@test.com", "owner");
        Playlist saved = playlist(uuid(10), owner, "title", "description", PlaylistStatus.ACTIVE);
        PlaylistDto dto = playlistDto(saved, false, List.of());

        authenticate(userId);
        given(userRepository.findById(userId)).willReturn(Optional.of(owner));
        given(playlistRepository.save(any(Playlist.class))).willReturn(saved);
        given(playlistMapper.toDto(saved, false)).willReturn(dto);

        PlaylistDto response = playlistService.create(new PlaylistCreateRequest("title", "description"));

        assertThat(response).isEqualTo(dto);
        // 저장 전 엔티티 상태를 캡처해서 소유자와 ACTIVE 상태가 실제로 조립됐는지 검증한다.
        ArgumentCaptor<Playlist> playlistCaptor = ArgumentCaptor.forClass(Playlist.class);
        then(playlistRepository).should().save(playlistCaptor.capture());
        Playlist captured = playlistCaptor.getValue();
        assertThat(captured.getOwner()).isEqualTo(owner);
        assertThat(captured.getTitle()).isEqualTo("title");
        assertThat(captured.getDescription()).isEqualTo("description");
        assertThat(captured.getStatus()).isEqualTo(PlaylistStatus.ACTIVE);
    }

    @Test
    @DisplayName("update changes own playlist")
    void update_success() {
        UUID userId = uuid(1);
        UUID playlistId = uuid(10);
        Playlist playlist = playlist(playlistId, user(userId, "owner@test.com", "owner"),
                "before", "before description", PlaylistStatus.ACTIVE);
        PlaylistDto dto = playlistDto(playlist, true, List.of());

        authenticate(userId);
        given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));
        given(playlistSubscriptionRepository.existsByPlaylistIdAndUserId(playlistId, userId))
                .willReturn(true);
        given(playlistMapper.toDto(playlist, true)).willReturn(dto);

        PlaylistDto response = playlistService.update(
                playlistId,
                new PlaylistUpdateRequest("after", "after description")
        );

        assertThat(response).isEqualTo(dto);
        assertThat(playlist.getTitle()).isEqualTo("after");
        assertThat(playlist.getDescription()).isEqualTo("after description");
    }

    @Test
    @DisplayName("findById returns subscription flag")
    void findById_success() {
        UUID userId = uuid(1);
        UUID playlistId = uuid(10);
        Playlist playlist = playlist(playlistId, user(uuid(2), "owner@test.com", "owner"),
                "title", "description", PlaylistStatus.ACTIVE);
        PlaylistDto dto = playlistDto(playlist, true, List.of());

        authenticate(userId);
        given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));
        given(playlistSubscriptionRepository.existsByPlaylistIdAndUserId(playlistId, userId))
                .willReturn(true);
        given(playlistMapper.toDto(playlist, true)).willReturn(dto);

        PlaylistDto response = playlistService.findById(playlistId);

        assertThat(response).isEqualTo(dto);
    }

    @Test
    @DisplayName("findAll returns playlist page with contents")
    void findAll_success() {
        UUID requestUserId = uuid(1);
        UUID ownerId = uuid(2);
        UUID playlistId = uuid(10);
        UUID contentId = uuid(20);
        User owner = user(ownerId, "owner@test.com", "owner");
        Playlist playlist = playlist(playlistId, owner, "playlist", "description", PlaylistStatus.ACTIVE);
        Content content = content(contentId, "movie");
        PlaylistContent playlistContent = new PlaylistContent(playlist, content);
        ContentStats stats = contentStats(content, BigDecimal.valueOf(4.25), 7);
        setUpdatedAt(playlist, "2026-06-29T00:00:00Z");

        PlaylistFindAllRequest request = new PlaylistFindAllRequest(
                null,
                null,
                null,
                null,
                null,
                1,
                "DESCENDING",
                "updatedAt"
        );

        authenticate(requestUserId);
        given(playlistRepository.search(request))
                .willReturn(new PlaylistRepositoryCustom.PlaylistSearchResult(List.of(playlist), 1L));
        // 목록 응답은 구독 여부, 콘텐츠, 통계, 태그를 한 번씩 모아 DTO로 조립한다.
        given(playlistSubscriptionRepository.findSubscribedPlaylistIds(requestUserId, List.of(playlistId)))
                .willReturn(Set.of(playlistId));
        given(playlistContentRepository.findAllWithPlaylistAndContentByPlaylistIds(List.of(playlistId)))
                .willReturn(List.of(playlistContent));
        given(contentStatsRepository.findByIdIn(List.of(contentId))).willReturn(List.of(stats));
        given(contentTagRepository.findTagsByContentIds(List.of(contentId)))
                .willReturn(List.of(new ContentTagProjection(contentId, "action")));

        var response = playlistService.findAll(request);

        assertThat(response.data()).hasSize(1);
        assertThat(response.data().get(0).id()).isEqualTo(playlistId);
        assertThat(response.data().get(0).subscribedByMe()).isTrue();
        assertThat(response.data().get(0).contents()).hasSize(1);
        ContentSummary summary = response.data().get(0).contents().get(0);
        assertThat(summary.id()).isEqualTo(contentId);
        assertThat(summary.tags()).containsExactly("action");
        assertThat(summary.averageRating()).isEqualTo(4.25);
        assertThat(summary.reviewCount()).isEqualTo(7);
        assertThat(response.totalCount()).isEqualTo(1L);
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    @DisplayName("findAll builds updatedAt cursor when result has next page")
    void findAll_hasNextUpdatedAtCursor() {
        UUID requestUserId = uuid(1);
        User owner = user(uuid(2), "owner@test.com", "owner");
        Playlist first = playlist(uuid(10), owner, "first", "description", PlaylistStatus.ACTIVE);
        Playlist second = playlist(uuid(11), owner, "second", "description", PlaylistStatus.ACTIVE);
        Playlist extra = playlist(uuid(12), owner, "extra", "description", PlaylistStatus.ACTIVE);
        setUpdatedAt(first, "2026-06-29T00:00:00Z");
        setUpdatedAt(second, "2026-06-29T00:01:00Z");
        setUpdatedAt(extra, "2026-06-29T00:02:00Z");

        PlaylistFindAllRequest request = new PlaylistFindAllRequest(
                null,
                null,
                null,
                null,
                null,
                2,
                "ASCENDING",
                "updatedAt"
        );
        List<UUID> pagePlaylistIds = List.of(first.getId(), second.getId());

        authenticate(requestUserId);
        // limit + 1개가 반환되면 extra는 잘라내고 현재 페이지의 마지막 항목으로 다음 커서를 만든다.
        given(playlistRepository.search(request))
                .willReturn(new PlaylistRepositoryCustom.PlaylistSearchResult(
                        List.of(first, second, extra),
                        3L
                ));
        given(playlistSubscriptionRepository.findSubscribedPlaylistIds(requestUserId, pagePlaylistIds))
                .willReturn(Set.of());
        given(playlistContentRepository.findAllWithPlaylistAndContentByPlaylistIds(pagePlaylistIds))
                .willReturn(List.of());

        var response = playlistService.findAll(request);

        assertThat(response.data()).extracting(PlaylistDto::id)
                .containsExactly(first.getId(), second.getId());
        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextCursor()).isEqualTo("2026-06-29T00:01:00Z");
        assertThat(response.nextIdAfter()).isEqualTo(second.getId());
        assertThat(response.totalCount()).isEqualTo(3L);
        assertThat(response.sortBy()).isEqualTo("updatedAt");
        assertThat(response.sortDirection()).isEqualTo("ASCENDING");
    }

    @Test
    @DisplayName("findAll builds subscribeCount cursor when result has next page")
    void findAll_hasNextSubscribeCountCursor() {
        UUID requestUserId = uuid(1);
        User owner = user(uuid(2), "owner@test.com", "owner");
        Playlist first = playlist(uuid(10), owner, "first", "description", PlaylistStatus.ACTIVE);
        Playlist second = playlist(uuid(11), owner, "second", "description", PlaylistStatus.ACTIVE);
        Playlist extra = playlist(uuid(12), owner, "extra", "description", PlaylistStatus.ACTIVE);
        setSubscriberCount(first, 10);
        setSubscriberCount(second, 7);
        setSubscriberCount(extra, 5);

        PlaylistFindAllRequest request = new PlaylistFindAllRequest(
                null,
                null,
                null,
                null,
                null,
                2,
                "DESCENDING",
                "subscribeCount"
        );
        List<UUID> pagePlaylistIds = List.of(first.getId(), second.getId());

        authenticate(requestUserId);
        // subscribeCount 정렬은 updatedAt 대신 마지막 응답 항목의 구독자 수를 커서 문자열로 사용한다.
        given(playlistRepository.search(request))
                .willReturn(new PlaylistRepositoryCustom.PlaylistSearchResult(
                        List.of(first, second, extra),
                        3L
                ));
        given(playlistSubscriptionRepository.findSubscribedPlaylistIds(requestUserId, pagePlaylistIds))
                .willReturn(Set.of(second.getId()));
        given(playlistContentRepository.findAllWithPlaylistAndContentByPlaylistIds(pagePlaylistIds))
                .willReturn(List.of());

        var response = playlistService.findAll(request);

        assertThat(response.data()).extracting(PlaylistDto::id)
                .containsExactly(first.getId(), second.getId());
        assertThat(response.data().get(0).subscriberCount()).isEqualTo(10L);
        assertThat(response.data().get(1).subscriberCount()).isEqualTo(7L);
        assertThat(response.data().get(1).subscribedByMe()).isTrue();
        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextCursor()).isEqualTo("7");
        assertThat(response.nextIdAfter()).isEqualTo(second.getId());
        assertThat(response.sortBy()).isEqualTo("subscribeCount");
        assertThat(response.sortDirection()).isEqualTo("DESCENDING");
    }

    @Test
    @DisplayName("subscribe inserts subscription and increases count")
    void subscribe_success() {
        UUID userId = uuid(1);
        UUID playlistId = uuid(10);
        Playlist playlist = playlist(playlistId, user(uuid(2), "owner@test.com", "owner"),
                "title", "description", PlaylistStatus.ACTIVE);
        User user = user(userId, "me@test.com", "me");

        authenticate(userId);
        given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(playlistSubscriptionRepository.insertIfNotExists(playlist, user)).willReturn(1);
        given(playlistRepository.increaseSubscriberCount(playlistId, PlaylistStatus.DELETED))
                .willReturn(1);

        playlistService.subscribe(playlistId);

        then(playlistRepository).should().increaseSubscriberCount(playlistId, PlaylistStatus.DELETED);
    }

    @Test
    @DisplayName("subscribe is idempotent for duplicate subscription")
    void subscribe_duplicate() {
        UUID userId = uuid(1);
        UUID playlistId = uuid(10);
        Playlist playlist = playlist(playlistId, user(uuid(2), "owner@test.com", "owner"),
                "title", "description", PlaylistStatus.ACTIVE);
        User user = user(userId, "me@test.com", "me");

        authenticate(userId);
        given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(playlistSubscriptionRepository.insertIfNotExists(playlist, user)).willReturn(0);

        playlistService.subscribe(playlistId);

        // 이미 구독 중이면 insert가 0을 반환하고 구독자 수 증가는 수행하지 않는다.
        then(playlistRepository).should(never()).increaseSubscriberCount(any(), any());
    }

    @Test
    @DisplayName("subscribe rejects playlist owner")
    void subscribe_ownerRejected() {
        UUID userId = uuid(1);
        UUID playlistId = uuid(10);
        Playlist playlist = playlist(playlistId, user(userId, "owner@test.com", "owner"),
                "title", "description", PlaylistStatus.ACTIVE);

        authenticate(userId);
        given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));

        assertThatThrownBy(() -> playlistService.subscribe(playlistId))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE)
                );
        then(userRepository).should(never()).findById(any());
    }

    @Test
    @DisplayName("unsubscribe deletes subscription and decreases count")
    void unsubscribe_success() {
        UUID userId = uuid(1);
        UUID playlistId = uuid(10);
        Playlist playlist = playlist(playlistId, user(uuid(2), "owner@test.com", "owner"),
                "title", "description", PlaylistStatus.ACTIVE);

        authenticate(userId);
        given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));
        given(playlistSubscriptionRepository.deleteByPlaylistIdAndUserId(playlistId, userId))
                .willReturn(1);
        given(playlistRepository.decreaseSubscriberCount(playlistId, PlaylistStatus.DELETED))
                .willReturn(1);

        playlistService.unsubscribe(playlistId);

        then(playlistRepository).should().decreaseSubscriberCount(playlistId, PlaylistStatus.DELETED);
    }

    @Test
    @DisplayName("unsubscribe is idempotent when subscription does not exist")
    void unsubscribe_duplicate() {
        UUID userId = uuid(1);
        UUID playlistId = uuid(10);
        Playlist playlist = playlist(playlistId, user(uuid(2), "owner@test.com", "owner"),
                "title", "description", PlaylistStatus.ACTIVE);

        authenticate(userId);
        given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));
        given(playlistSubscriptionRepository.deleteByPlaylistIdAndUserId(playlistId, userId))
                .willReturn(0);

        playlistService.unsubscribe(playlistId);

        // 이미 구독 취소된 상태이면 삭제 수가 0이고 구독자 수 감소도 수행하지 않는다.
        then(playlistRepository).should(never()).decreaseSubscriberCount(any(), any());
    }

    @Test
    @DisplayName("addContent inserts content for owner")
    void addContent_success() {
        UUID userId = uuid(1);
        UUID playlistId = uuid(10);
        UUID contentId = uuid(20);
        Playlist playlist = playlist(playlistId, user(userId, "owner@test.com", "owner"),
                "title", "description", PlaylistStatus.ACTIVE);
        Content content = content(contentId, "movie");

        authenticate(userId);
        given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));
        given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
        given(playlistContentRepository.insertIfNotExists(playlist, content)).willReturn(1);

        playlistService.addContent(playlistId, contentId);

        then(playlistContentRepository).should().insertIfNotExists(playlist, content);
    }

    @Test
    @DisplayName("removeContent rejects missing content")
    void removeContent_missingContent() {
        UUID userId = uuid(1);
        UUID playlistId = uuid(10);
        UUID contentId = uuid(20);
        Playlist playlist = playlist(playlistId, user(userId, "owner@test.com", "owner"),
                "title", "description", PlaylistStatus.ACTIVE);

        authenticate(userId);
        given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));
        given(contentRepository.existsById(contentId)).willReturn(false);

        assertThatThrownBy(() -> playlistService.removeContent(playlistId, contentId))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONTENT_NOT_FOUND)
                );
        then(playlistContentRepository).should(never()).deleteByPlaylistIdAndContentId(any(), any());
    }

    @Test
    @DisplayName("delete soft-deletes own active playlist")
    void delete_success() {
        UUID userId = uuid(1);
        UUID playlistId = uuid(10);
        Playlist playlist = playlist(playlistId, user(userId, "owner@test.com", "owner"),
                "title", "description", PlaylistStatus.ACTIVE);

        authenticate(userId);
        given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));

        playlistService.delete(playlistId);

        assertThat(playlist.getStatus()).isEqualTo(PlaylistStatus.DELETED);
        assertThat(playlist.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("hardDelete rejects non-owner")
    void hardDelete_ownerMismatch() {
        UUID requestUserId = uuid(1);
        UUID playlistId = uuid(10);
        Playlist playlist = playlist(playlistId, user(uuid(2), "owner@test.com", "owner"),
                "title", "description", PlaylistStatus.ACTIVE);

        authenticate(requestUserId);
        given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));

        assertThatThrownBy(() -> playlistService.hardDelete(playlistId))
                .isInstanceOfSatisfying(PlaylistOwnerMismatchException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED)
                );
        then(playlistRepository).should(never()).delete(any());
    }

    @Test
    @DisplayName("findById rejects deleted playlist")
    void findById_deletedPlaylist() {
        UUID userId = uuid(1);
        UUID playlistId = uuid(10);
        Playlist playlist = playlist(playlistId, user(uuid(2), "owner@test.com", "owner"),
                "title", "description", PlaylistStatus.DELETED);

        authenticate(userId);
        given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));

        assertThatThrownBy(() -> playlistService.findById(playlistId))
                .isInstanceOf(PlaylistNotFoundException.class);
    }

    private void authenticate(UUID userId) {
        // 서비스가 SecurityUtils.getCurrentUserId()를 호출하므로 실제 인증 컨텍스트에 AuthUser를 심는다.
        AuthUser authUser = new AuthUser(userId, UserRole.USER, uuid(99));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(authUser, null, authUser.authorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private User user(UUID id, String email, String name) {
        User user = new User(email, name, "password", null, UserRole.USER);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Playlist playlist(
            UUID id,
            User owner,
            String title,
            String description,
            PlaylistStatus status
    ) {
        Playlist playlist = Playlist.builder()
                .owner(owner)
                .title(title)
                .description(description)
                .status(status)
                .build();
        // JPA가 채우는 식별자와 감사 시간을 단위 테스트에서 필요한 값으로 고정한다.
        ReflectionTestUtils.setField(playlist, "id", id);
        ReflectionTestUtils.setField(playlist, "createdAt", Instant.parse("2026-06-29T00:00:00Z"));
        ReflectionTestUtils.setField(playlist, "updatedAt", Instant.parse("2026-06-29T00:00:00Z"));
        return playlist;
    }

    private Content content(UUID id, String title) {
        Content content = Content.builder()
                .type(ContentType.MOVIE)
                .title(title)
                .description("description")
                .thumbnailUrl("thumbnail")
                .externalId("external-" + id)
                .source("tmdb")
                .build();
        ReflectionTestUtils.setField(content, "id", id);
        return content;
    }

    private ContentStats contentStats(Content content, BigDecimal averageRating, int reviewCount) {
        ContentStats stats = ContentStats.builder()
                .content(content)
                .averageRating(averageRating)
                .reviewCount(reviewCount)
                .viewerCount(0)
                .build();
        ReflectionTestUtils.setField(stats, "id", content.getId());
        return stats;
    }

    private PlaylistDto playlistDto(Playlist playlist, boolean subscribedByMe, List<ContentSummary> contents) {
        return new PlaylistDto(
                playlist.getId(),
                null,
                playlist.getTitle(),
                playlist.getDescription(),
                playlist.getUpdatedAt(),
                playlist.getSubscriberCount().longValue(),
                subscribedByMe,
                contents
        );
    }

    private void setUpdatedAt(Playlist playlist, String updatedAt) {
        // 커서 조립 분기를 검증하기 위해 updatedAt을 테스트별로 다르게 지정한다.
        ReflectionTestUtils.setField(playlist, "updatedAt", Instant.parse(updatedAt));
    }

    private void setSubscriberCount(Playlist playlist, int subscriberCount) {
        // subscribeCount 정렬 커서가 마지막 응답 항목의 값을 쓰는지 확인하기 위해 명시적으로 지정한다.
        ReflectionTestUtils.setField(playlist, "subscriberCount", subscriberCount);
    }

    private UUID uuid(int value) {
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", value));
    }
}
