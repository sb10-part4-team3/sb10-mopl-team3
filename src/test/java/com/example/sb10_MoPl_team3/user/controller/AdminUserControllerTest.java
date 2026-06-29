package com.example.sb10_MoPl_team3.user.controller;

import com.example.sb10_MoPl_team3.global.config.SecurityConfig;
import com.example.sb10_MoPl_team3.global.cursor.CursorResponse;
import com.example.sb10_MoPl_team3.global.exception.GlobalExceptionHandler;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtProvider;
import com.example.sb10_MoPl_team3.user.dto.request.UserRoleUpdateRequest;
import com.example.sb10_MoPl_team3.user.dto.request.UserSearchCondition;
import com.example.sb10_MoPl_team3.user.dto.response.UserDto;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.service.AdminUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminUserController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminUserService adminUserService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("관리자는 사용자 목록을 조건과 커서 페이지네이션으로 조회할 수 있다")
    void findUsers_success() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UUID nextIdAfter = UUID.randomUUID();

        UserDto userDto = new UserDto(
                userId,
                Instant.parse("2026-06-28T00:00:00Z"),
                "user@test.com",
                "User",
                null,
                UserRole.USER,
                false
        );

        CursorResponse<UserDto> response = new CursorResponse<>(
                List.of(userDto),
                "user@test.com",
                nextIdAfter,
                true,
                1L,
                "email",
                "ASCENDING"
        );

        given(adminUserService.findUsers(any(UserSearchCondition.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/users")
                        .with(user("admin").roles("ADMIN"))
                        .param("emailLike", "test")
                        .param("roleEqual", "USER")
                        .param("isLocked", "false")
                        .param("cursor", "cursor-value")
                        .param("idAfter", nextIdAfter.toString())
                        .param("limit", "20")
                        .param("sortDirection", "ASCENDING")
                        .param("sortBy", "email"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(userId.toString()))
                .andExpect(jsonPath("$.data[0].email").value("user@test.com"))
                .andExpect(jsonPath("$.data[0].name").value("User"))
                .andExpect(jsonPath("$.data[0].role").value("USER"))
                .andExpect(jsonPath("$.data[0].locked").value(false))
                .andExpect(jsonPath("$.nextCursor").value("user@test.com"))
                .andExpect(jsonPath("$.nextIdAfter").value(nextIdAfter.toString()))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.sortBy").value("email"))
                .andExpect(jsonPath("$.sortDirection").value("ASCENDING"));

        ArgumentCaptor<UserSearchCondition> conditionCaptor =
                ArgumentCaptor.forClass(UserSearchCondition.class);

        then(adminUserService).should().findUsers(conditionCaptor.capture());

        UserSearchCondition condition = conditionCaptor.getValue();

        assertThat(condition.emailLike()).isEqualTo("test");
        assertThat(condition.roleEqual()).isEqualTo(UserRole.USER);
        assertThat(condition.isLocked()).isFalse();
        assertThat(condition.cursor()).isEqualTo("cursor-value");
        assertThat(condition.idAfter()).isEqualTo(nextIdAfter);
        assertThat(condition.limit()).isEqualTo(20);
        assertThat(condition.sortDirection()).isEqualTo("ASCENDING");
        assertThat(condition.sortBy()).isEqualTo("email");
    }

    @Test
    @DisplayName("일반 사용자는 사용자 목록을 조회할 수 없다")
    void findUsers_forbidden() throws Exception {
        mockMvc.perform(get("/api/users")
                        .with(user("user").roles("USER"))
                        .param("limit", "20")
                        .param("sortDirection", "ASCENDING")
                        .param("sortBy", "email"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 사용자 목록을 조회할 수 없다")
    void findUsers_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("limit", "20")
                        .param("sortDirection", "ASCENDING")
                        .param("sortBy", "email"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("관리자는 사용자의 권한을 변경할 수 있다")
    void updateUserRole_success() throws Exception {
        UUID userId = UUID.randomUUID();

        UserRoleUpdateRequest request = new UserRoleUpdateRequest(UserRole.ADMIN);

        UserDto response = new UserDto(
                userId,
                Instant.parse("2026-06-28T00:00:00Z"),
                "user@test.com",
                "User",
                null,
                UserRole.ADMIN,
                false
        );

        given(adminUserService.updateUserRole(any(UUID.class), any(UserRoleUpdateRequest.class)))
                .willReturn(response);

        mockMvc.perform(patch("/api/users/{userId}/role", userId)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("user@test.com"))
                .andExpect(jsonPath("$.name").value("User"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.locked").value(false));

        ArgumentCaptor<UUID> userIdCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<UserRoleUpdateRequest> requestCaptor =
                ArgumentCaptor.forClass(UserRoleUpdateRequest.class);

        then(adminUserService).should()
                .updateUserRole(userIdCaptor.capture(), requestCaptor.capture());

        assertThat(userIdCaptor.getValue()).isEqualTo(userId);
        assertThat(requestCaptor.getValue().role()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    @DisplayName("일반 사용자는 사용자의 권한을 변경할 수 없다")
    void updateUserRole_forbidden() throws Exception {
        UUID userId = UUID.randomUUID();

        UserRoleUpdateRequest request = new UserRoleUpdateRequest(UserRole.ADMIN);

        mockMvc.perform(patch("/api/users/{userId}/role", userId)
                        .with(user("user").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        then(adminUserService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 사용자의 권한을 변경할 수 없다")
    void updateUserRole_unauthenticated() throws Exception {
        UUID userId = UUID.randomUUID();

        UserRoleUpdateRequest request = new UserRoleUpdateRequest(UserRole.ADMIN);

        mockMvc.perform(patch("/api/users/{userId}/role", userId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        then(adminUserService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("변경할 권한 값이 없으면 권한 변경 요청에 실패한다")
    void updateUserRole_invalidRole() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(patch("/api/users/{userId}/role", userId)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "role": null
                            }
                            """))
                .andExpect(status().isBadRequest());

        then(adminUserService).shouldHaveNoInteractions();
    }
}