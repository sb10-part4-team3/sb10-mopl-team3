package com.example.sb10_MoPl_team3.watchingsession.controller;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.dto.ContentSummary;
import com.example.sb10_MoPl_team3.global.config.SecurityConfig;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.global.exception.GlobalExceptionHandler;
import com.example.sb10_MoPl_team3.global.security.AuthUser;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtProvider;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtSessionValidator;
import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionDto;
import com.example.sb10_MoPl_team3.watchingsession.dto.CursorResponseWatchingSessionDto;
import com.example.sb10_MoPl_team3.watchingsession.service.WatchingSessionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.argThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WatchingSessionController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class WatchingSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WatchingSessionService watchingSessionService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private JwtSessionValidator jwtSessionValidator;

    @Test
    @DisplayName("특정 사용자의 실시간 시청 세션을 WatchingSessionDto로 반환한다")
    void findWatchingSessionByWatcher_success() throws Exception {
        UUID watcherId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID sessionId = UUID.fromString("00000000-0000-0000-0000-000000000011");
        UUID contentId = UUID.fromString("00000000-0000-0000-0000-000000000021");
        WatchingSessionDto response = new WatchingSessionDto(
                sessionId,
                Instant.parse("2026-06-29T00:00:00Z"),
                new UserSummary(watcherId, "사용자", null),
                new ContentSummary(
                        contentId,
                        ContentType.MOVIE,
                        "콘텐츠",
                        "설명",
                        "https://example.com/thumbnail.jpg",
                        List.of("tag"),
                        4.5,
                        3
                )
        );

        given(watchingSessionService.findByWatcher(watcherId)).willReturn(response);

        mockMvc.perform(get("/api/users/{watcherId}/watching-sessions", watcherId)
                        .with(authentication(authToken(watcherId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId.toString()))
                .andExpect(jsonPath("$.createdAt").value("2026-06-29T00:00:00Z"))
                .andExpect(jsonPath("$.watcher.userId").value(watcherId.toString()))
                .andExpect(jsonPath("$.watcher.name").value("사용자"))
                .andExpect(jsonPath("$.watcher.profileImageUrl").doesNotExist())
                .andExpect(jsonPath("$.content.id").value(contentId.toString()))
                .andExpect(jsonPath("$.content.type").value("MOVIE"))
                .andExpect(jsonPath("$.content.title").value("콘텐츠"))
                .andExpect(jsonPath("$.content.tags[0]").value("tag"))
                .andExpect(jsonPath("$.content.averageRating").value(4.5))
                .andExpect(jsonPath("$.content.reviewCount").value(3));
    }

    @Test
    @DisplayName("특정 사용자가 시청 중이 아니면 200과 빈 본문을 반환한다")
    void findWatchingSessionByWatcher_notWatching() throws Exception {
        UUID watcherId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        given(watchingSessionService.findByWatcher(watcherId)).willReturn(null);

        mockMvc.perform(get("/api/users/{watcherId}/watching-sessions", watcherId)
                        .with(authentication(authToken(watcherId))))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("조회 대상 사용자가 없으면 404를 반환한다")
    void findWatchingSessionByWatcher_userNotFound() throws Exception {
        UUID watcherId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        given(watchingSessionService.findByWatcher(watcherId))
                .willThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

        mockMvc.perform(get("/api/users/{watcherId}/watching-sessions", watcherId)
                        .with(authentication(authToken(watcherId))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    @DisplayName("특정 콘텐츠의 시청 세션 목록을 커서 응답 규격으로 반환한다")
    void findWatchingSessionsByContent_success() throws Exception {
        UUID requesterId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID contentId = UUID.fromString("00000000-0000-0000-0000-000000000021");
        UUID sessionId = UUID.fromString("00000000-0000-0000-0000-000000000011");
        UUID nextId = UUID.fromString("00000000-0000-0000-0000-000000000012");
        WatchingSessionDto session = new WatchingSessionDto(
                sessionId,
                Instant.parse("2026-06-30T00:00:00Z"),
                new UserSummary(requesterId, "시청자", null),
                new ContentSummary(contentId, ContentType.MOVIE, "콘텐츠", "설명",
                        "thumbnail", List.of(), 0.0, 0)
        );
        var response = new CursorResponseWatchingSessionDto(
                List.of(session), "2026-06-30T00:00:00Z", nextId,
                true, 3L, "createdAt", "DESCENDING"
        );
        given(watchingSessionService.findByContent(argThat(request ->
                request.contentId().equals(contentId)
                        && "시청".equals(request.watcherNameLike())
                        && request.limit() == 1
                        && "DESCENDING".equals(request.sortDirection())
                        && "createdAt".equals(request.sortBy())))).willReturn(response);

        mockMvc.perform(get("/api/contents/{contentId}/watching-sessions", contentId)
                        .param("watcherNameLike", "시청")
                        .param("limit", "1")
                        .param("sortDirection", "DESCENDING")
                        .param("sortBy", "createdAt")
                        .with(authentication(authToken(requesterId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(sessionId.toString()))
                .andExpect(jsonPath("$.nextCursor").value("2026-06-30T00:00:00Z"))
                .andExpect(jsonPath("$.nextIdAfter").value(nextId.toString()))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.totalCount").value(3))
                .andExpect(jsonPath("$.sortBy").value("createdAt"))
                .andExpect(jsonPath("$.sortDirection").value("DESCENDING"));
    }

    @Test
    @DisplayName("존재하지 않는 콘텐츠의 시청 세션 목록 조회 시 404를 반환한다")
    void findWatchingSessionsByContent_contentNotFound() throws Exception {
        UUID requesterId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID contentId = UUID.fromString("00000000-0000-0000-0000-000000000021");
        given(watchingSessionService.findByContent(argThat(request ->
                request.contentId().equals(contentId))))
                .willThrow(new BusinessException(ErrorCode.CONTENT_NOT_FOUND));

        mockMvc.perform(get("/api/contents/{contentId}/watching-sessions", contentId)
                        .param("limit", "20")
                        .param("sortDirection", "DESCENDING")
                        .param("sortBy", "createdAt")
                        .with(authentication(authToken(requesterId))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CONTENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("잘못된 커서로 시청 세션 목록 조회 시 400을 반환한다")
    void findWatchingSessionsByContent_invalidCursor() throws Exception {
        UUID requesterId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID contentId = UUID.fromString("00000000-0000-0000-0000-000000000021");
        UUID idAfter = UUID.fromString("00000000-0000-0000-0000-000000000011");
        given(watchingSessionService.findByContent(argThat(request ->
                contentId.equals(request.contentId())
                        && "invalid-cursor".equals(request.cursor())
                        && idAfter.equals(request.idAfter()))))
                .willThrow(new BusinessException(ErrorCode.INVALID_CURSOR));

        mockMvc.perform(get("/api/contents/{contentId}/watching-sessions", contentId)
                        .param("cursor", "invalid-cursor")
                        .param("idAfter", idAfter.toString())
                        .param("limit", "20")
                        .param("sortDirection", "DESCENDING")
                        .param("sortBy", "createdAt")
                        .with(authentication(authToken(requesterId))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CURSOR"));
    }

    @Test
    @DisplayName("필수 페이징 파라미터가 누락되면 400을 반환한다")
    void findWatchingSessionsByContent_missingRequiredParameter() throws Exception {
        UUID requesterId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID contentId = UUID.fromString("00000000-0000-0000-0000-000000000021");

        mockMvc.perform(get("/api/contents/{contentId}/watching-sessions", contentId)
                        .param("sortDirection", "DESCENDING")
                        .param("sortBy", "createdAt")
                        .with(authentication(authToken(requesterId))))
                .andExpect(status().isBadRequest());
    }

    private UsernamePasswordAuthenticationToken authToken(UUID userId) {
        AuthUser authUser = new AuthUser(userId, UserRole.USER, UUID.randomUUID());
        return new UsernamePasswordAuthenticationToken(
                authUser,
                null,
                authUser.authorities()
        );
    }
}
