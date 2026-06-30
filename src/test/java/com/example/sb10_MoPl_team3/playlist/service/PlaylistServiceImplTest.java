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
        ReflectionTestUtils.setField(playlist, "updatedAt", Instant.parse(updatedAt));
    }

    private UUID uuid(int value) {
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", value));
    }
}
