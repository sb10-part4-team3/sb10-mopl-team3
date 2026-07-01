package com.example.sb10_MoPl_team3.user.controller;

import com.example.sb10_MoPl_team3.global.security.jwt.JwtProvider;
import com.example.sb10_MoPl_team3.user.dto.response.UserDto;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.service.UserService;
import com.example.sb10_MoPl_team3.global.config.SecurityConfig;
import com.example.sb10_MoPl_team3.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.example.sb10_MoPl_team3.global.security.exception.AccessDeniedBusinessException;
import com.example.sb10_MoPl_team3.user.dto.request.UserUpdateRequest;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("회원가입 요청이 유효하면 사용자를 생성하고 201을 반환한다")
    void createUser_success() throws Exception {
        UserDto response = new UserDto(
                UUID.randomUUID(),
                Instant.parse("2026-06-23T00:00:00Z"),
                "user@test.com",
                "홍길동",
                null,
                UserRole.USER,
                false
        );

        given(userService.createUser(any())).willReturn(response);

        mockMvc.perform(post("/api/users")
                        .contentType(APPLICATION_JSON)
                        .with(csrf())
                        .content("""
                                {
                                  "name": "홍길동",
                                  "email": "user@test.com",
                                  "password": "password1!"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.email").value("user@test.com"))
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.profileImageUrl").doesNotExist())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.locked").value(false));
    }

    @Test
    @DisplayName("회원가입 요청 값이 유효하지 않으면 400을 반환한다")
    void createUser_invalid() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(APPLICATION_JSON)
                        .with(csrf())
                        .content("""
                                {
                                  "name": "",
                                  "email": "invalid-email",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    @DisplayName("인증된 사용자는 사용자 상세 정보를 조회할 수 있다")
    void findUser_success() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        UserDto response = new UserDto(
                userId,
                Instant.parse("2026-06-29T00:00:00Z"),
                "user@test.com",
                "홍길동",
                "https://image.test/profile.png",
                UserRole.USER,
                false
        );

        given(userService.findUser(userId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/users/{userId}", userId)
                        .with(user("user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.email").value("user@test.com"))
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.profileImageUrl").value("https://image.test/profile.png"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.locked").value(false));

        then(userService).should().findUser(userId);
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 사용자 상세 정보를 조회할 수 없다")
    void findUser_unauthenticated() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        // when & then
        mockMvc.perform(get("/api/users/{userId}", userId))
                .andExpect(status().isUnauthorized());

        then(userService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("인증된 사용자는 본인 프로필을 수정할 수 있다")
    void updateUser_success() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        MockMultipartFile requestPart = new MockMultipartFile(
                "request",
                "",
                "application/json",
                """
                {
                  "name": "변경된이름"
                }
                """.getBytes(StandardCharsets.UTF_8)
        );

        MockMultipartFile imagePart = new MockMultipartFile(
                "image",
                "profile.png",
                "image/png",
                "image".getBytes(StandardCharsets.UTF_8)
        );

        UserDto response = new UserDto(
                userId,
                Instant.parse("2026-06-29T00:00:00Z"),
                "user@test.com",
                "변경된이름",
                "https://image.test/profile.png",
                UserRole.USER,
                false
        );

        given(userService.updateUser(eq(userId), any(UserUpdateRequest.class), any(MultipartFile.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(multipart("/api/users/{userId}", userId)
                        .file(requestPart)
                        .file(imagePart)
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        })
                        .with(user("user").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("user@test.com"))
                .andExpect(jsonPath("$.name").value("변경된이름"))
                .andExpect(jsonPath("$.profileImageUrl").value("https://image.test/profile.png"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.locked").value(false));

        ArgumentCaptor<UserUpdateRequest> requestCaptor =
                ArgumentCaptor.forClass(UserUpdateRequest.class);
        ArgumentCaptor<MultipartFile> imageCaptor =
                ArgumentCaptor.forClass(MultipartFile.class);

        then(userService).should()
                .updateUser(eq(userId), requestCaptor.capture(), imageCaptor.capture());

        assertThat(requestCaptor.getValue().name()).isEqualTo("변경된이름");
        assertThat(imageCaptor.getValue().getOriginalFilename()).isEqualTo("profile.png");
    }

    @Test
    @DisplayName("프로필 수정 요청에 이미지가 없어도 수정할 수 있다")
    void updateUser_withoutImage() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        MockMultipartFile requestPart = new MockMultipartFile(
                "request",
                "",
                "application/json",
                """
                {
                  "name": "변경된이름"
                }
                """.getBytes(StandardCharsets.UTF_8)
        );

        UserDto response = new UserDto(
                userId,
                Instant.parse("2026-06-29T00:00:00Z"),
                "user@test.com",
                "변경된이름",
                "https://image.test/old.png",
                UserRole.USER,
                false
        );

        given(userService.updateUser(eq(userId), any(UserUpdateRequest.class), eq(null)))
                .willReturn(response);

        // when & then
        mockMvc.perform(multipart("/api/users/{userId}", userId)
                        .file(requestPart)
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        })
                        .with(user("user").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("변경된이름"))
                .andExpect(jsonPath("$.profileImageUrl").value("https://image.test/old.png"));

        then(userService).should()
                .updateUser(eq(userId), any(UserUpdateRequest.class), eq(null));
    }

    @Test
    @DisplayName("본인이 아닌 사용자의 프로필을 수정하면 403을 반환한다")
    void updateUser_forbidden() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        MockMultipartFile requestPart = new MockMultipartFile(
                "request",
                "",
                "application/json",
                """
                {
                  "name": "변경된이름"
                }
                """.getBytes(StandardCharsets.UTF_8)
        );

        given(userService.updateUser(eq(userId), any(UserUpdateRequest.class), eq(null)))
                .willThrow(new AccessDeniedBusinessException());

        // when & then
        mockMvc.perform(multipart("/api/users/{userId}", userId)
                        .file(requestPart)
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        })
                        .with(user("user").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 프로필을 수정할 수 없다")
    void updateUser_unauthenticated() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        MockMultipartFile requestPart = new MockMultipartFile(
                "request",
                "",
                "application/json",
                """
                {
                  "name": "변경된이름"
                }
                """.getBytes(StandardCharsets.UTF_8)
        );

        // when & then
        mockMvc.perform(multipart("/api/users/{userId}", userId)
                        .file(requestPart)
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        })
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        then(userService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("CSRF 토큰 없이 프로필을 수정하면 403을 반환한다")
    void updateUser_withoutCsrf() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        MockMultipartFile requestPart = new MockMultipartFile(
                "request",
                "",
                "application/json",
                """
                {
                  "name": "변경된이름"
                }
                """.getBytes(StandardCharsets.UTF_8)
        );

        // when & then
        mockMvc.perform(multipart("/api/users/{userId}", userId)
                        .file(requestPart)
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        })
                        .with(user("user").roles("USER")))
                .andExpect(status().isForbidden());

        then(userService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("프로필 수정 요청 값이 유효하지 않으면 400을 반환한다")
    void updateUser_invalid() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        MockMultipartFile requestPart = new MockMultipartFile(
                "request",
                "",
                "application/json",
                """
                {
                  "name": ""
                }
                """.getBytes(StandardCharsets.UTF_8)
        );

        // when & then
        mockMvc.perform(multipart("/api/users/{userId}", userId)
                        .file(requestPart)
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        })
                        .with(user("user").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));

        then(userService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("인증된 사용자는 본인 계정을 탈퇴할 수 있다")
    void withdrawUser_success() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        // when & then
        mockMvc.perform(delete("/api/users/{userId}", userId)
                        .with(user("user").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        then(userService).should().withdrawUser(userId);
    }

    @Test
    @DisplayName("본인이 아닌 사용자의 계정을 탈퇴하려 하면 403을 반환한다")
    void withdrawUser_forbidden() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        willThrow(new AccessDeniedBusinessException())
                .given(userService).withdrawUser(userId);

        // when & then
        mockMvc.perform(delete("/api/users/{userId}", userId)
                        .with(user("user").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        then(userService).should().withdrawUser(userId);
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 계정을 탈퇴할 수 없다")
    void withdrawUser_unauthenticated() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        // when & then
        mockMvc.perform(delete("/api/users/{userId}", userId)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        then(userService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("CSRF 토큰 없이 계정을 탈퇴하면 403을 반환한다")
    void withdrawUser_withoutCsrf() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        // when & then
        mockMvc.perform(delete("/api/users/{userId}", userId)
                        .with(user("user").roles("USER")))
                .andExpect(status().isForbidden());

        then(userService).shouldHaveNoInteractions();
    }
}
