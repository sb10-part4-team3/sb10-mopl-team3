package com.example.sb10_MoPl_team3.follow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.sb10_MoPl_team3.follow.dto.FollowDto;
import com.example.sb10_MoPl_team3.follow.dto.FollowRequest;
import com.example.sb10_MoPl_team3.follow.service.FollowCreateResult;
import com.example.sb10_MoPl_team3.follow.service.FollowService;
import com.example.sb10_MoPl_team3.global.config.SecurityConfig;
import com.example.sb10_MoPl_team3.global.exception.GlobalExceptionHandler;
import com.example.sb10_MoPl_team3.global.security.AuthUser;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtProvider;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtSessionValidator;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FollowController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class FollowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FollowService followService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private JwtSessionValidator jwtSessionValidator;

    @Test
    @DisplayName("createFollow returns 201 when a follow is newly created")
    void createFollow_created() throws Exception {
        UUID followerId = uuid(1);
        UUID followeeId = uuid(2);
        UUID followId = uuid(10);
        FollowDto followDto = new FollowDto(followId, followeeId, followerId);

        given(followService.create(eq(followerId), any(FollowRequest.class)))
                .willReturn(new FollowCreateResult(followDto, true));

        mockMvc.perform(post("/api/follows")
                        .with(authentication(authToken(followerId)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "followeeId": "00000000-0000-0000-0000-000000000002"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(followId.toString()))
                .andExpect(jsonPath("$.followeeId").value(followeeId.toString()))
                .andExpect(jsonPath("$.followerId").value(followerId.toString()));
    }

    @Test
    @DisplayName("createFollow returns 200 when an existing follow is returned")
    void createFollow_existing() throws Exception {
        UUID followerId = uuid(1);
        UUID followeeId = uuid(2);
        UUID followId = uuid(10);
        FollowDto followDto = new FollowDto(followId, followeeId, followerId);

        given(followService.create(eq(followerId), any(FollowRequest.class)))
                .willReturn(new FollowCreateResult(followDto, false));

        mockMvc.perform(post("/api/follows")
                        .with(authentication(authToken(followerId)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "followeeId": "00000000-0000-0000-0000-000000000002"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(followId.toString()))
                .andExpect(jsonPath("$.followeeId").value(followeeId.toString()))
                .andExpect(jsonPath("$.followerId").value(followerId.toString()));
    }

    private UsernamePasswordAuthenticationToken authToken(UUID userId) {
        AuthUser authUser = new AuthUser(userId, UserRole.USER, UUID.randomUUID());
        return new UsernamePasswordAuthenticationToken(
                authUser,
                null,
                authUser.authorities()
        );
    }

    private UUID uuid(int value) {
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", value));
    }
}
