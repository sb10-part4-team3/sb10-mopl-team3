package com.example.sb10_MoPl_team3.watchingsession.controller;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.dto.ContentSummary;
import com.example.sb10_MoPl_team3.global.config.SecurityConfig;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.global.exception.GlobalExceptionHandler;
import com.example.sb10_MoPl_team3.global.security.AuthUser;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtProvider;
import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionDto;
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

    private UsernamePasswordAuthenticationToken authToken(UUID userId) {
        AuthUser authUser = new AuthUser(userId, UserRole.USER, null);
        return new UsernamePasswordAuthenticationToken(
                authUser,
                null,
                authUser.authorities()
        );
    }
}
