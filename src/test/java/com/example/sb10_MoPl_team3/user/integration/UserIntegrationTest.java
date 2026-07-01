package com.example.sb10_MoPl_team3.user.integration;

import com.example.sb10_MoPl_team3.auth.repository.AuthSessionRepository;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtClaims;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtProvider;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import com.example.sb10_MoPl_team3.user.enums.UserStatus;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Testcontainers(disabledWithoutDocker = true)
class UserIntegrationTest {

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthSessionRepository authSessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void cleanRedis() {
        authSessionRepository.deleteAll();
    }

    @Test
    @DisplayName("인증된 사용자는 사용자 상세 정보를 조회할 수 있다")
    void findUser_success() throws Exception {
        // given
        User targetUser = userRepository.save(new User(
                "target@test.com",
                "Target User",
                passwordEncoder.encode("password1!"),
                "https://image.test/profile.png",
                UserRole.USER
        ));

        String accessToken = createUserAndSignIn(
                "viewer@test.com",
                "Viewer",
                "password1!",
                UserRole.USER
        );

        // when & then
        mockMvc.perform(get("/api/users/{userId}", targetUser.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(targetUser.getId().toString()))
                .andExpect(jsonPath("$.email").value("target@test.com"))
                .andExpect(jsonPath("$.name").value("Target User"))
                .andExpect(jsonPath("$.profileImageUrl").value("https://image.test/profile.png"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.locked").value(false));
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 사용자 상세 정보를 조회할 수 없다")
    void findUser_unauthenticated() throws Exception {
        // given
        User targetUser = userRepository.save(new User(
                "target-unauth@test.com",
                "Target User",
                passwordEncoder.encode("password1!"),
                null,
                UserRole.USER
        ));

        // when & then
        mockMvc.perform(get("/api/users/{userId}", targetUser.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("인증된 사용자는 본인 프로필 이름을 수정할 수 있다")
    void updateUser_success() throws Exception {
        // given
        String accessToken = createUserAndSignIn(
                "profile@test.com",
                "Old Name",
                "password1!",
                UserRole.USER
        );

        JwtClaims claims = jwtProvider.parseAccessToken(accessToken);
        UUID userId = claims.userId();

        MockMultipartFile requestPart = new MockMultipartFile(
                "request",
                "",
                "application/json",
                """
                {
                  "name": "New Name"
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
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("profile@test.com"))
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.locked").value(false));

        User updatedUser = userRepository.findById(userId).orElseThrow();
        assertThat(updatedUser.getName()).isEqualTo("New Name");
    }

    @Test
    @DisplayName("다른 사용자의 프로필을 수정할 수 없다")
    void updateUser_forbidden() throws Exception {
        // given
        User targetUser = userRepository.save(new User(
                "target-profile@test.com",
                "Target User",
                passwordEncoder.encode("password1!"),
                null,
                UserRole.USER
        ));

        String accessToken = createUserAndSignIn(
                "other@test.com",
                "Other User",
                "password1!",
                UserRole.USER
        );

        MockMultipartFile requestPart = new MockMultipartFile(
                "request",
                "",
                "application/json",
                """
                {
                  "name": "Hacked Name"
                }
                """.getBytes(StandardCharsets.UTF_8)
        );

        // when & then
        mockMvc.perform(multipart("/api/users/{userId}", targetUser.getId())
                        .file(requestPart)
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        })
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        User unchangedUser = userRepository.findById(targetUser.getId()).orElseThrow();
        assertThat(unchangedUser.getName()).isEqualTo("Target User");
    }

    @Test
    @DisplayName("CSRF 토큰 없이 프로필을 수정할 수 없다")
    void updateUser_withoutCsrf() throws Exception {
        // given
        String accessToken = createUserAndSignIn(
                "csrf-profile@test.com",
                "User",
                "password1!",
                UserRole.USER
        );

        UUID userId = jwtProvider.parseAccessToken(accessToken).userId();

        MockMultipartFile requestPart = new MockMultipartFile(
                "request",
                "",
                "application/json",
                """
                {
                  "name": "New Name"
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
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("인증된 사용자는 본인 계정을 탈퇴할 수 있다")
    void withdrawUser_success() throws Exception {
        // given
        String accessToken = createUserAndSignIn(
                "withdraw@test.com",
                "Withdraw User",
                "password1!",
                UserRole.USER
        );

        UUID userId = jwtProvider.parseAccessToken(accessToken).userId();

        // when & then
        mockMvc.perform(delete("/api/users/{userId}", userId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        User withdrawnUser = userRepository.findById(userId).orElseThrow();
        assertThat(withdrawnUser.getStatus()).isEqualTo(UserStatus.WITHDRAWN);

        assertThat(authSessionRepository.findAllByUserId(userId))
                .isNotEmpty()
                .allSatisfy(session -> assertThat(session.isRevoked()).isTrue());
    }

    @Test
    @DisplayName("탈퇴한 계정은 다시 로그인할 수 없다")
    void withdrawUser_blocksSignIn() throws Exception {
        // given
        String email = "withdraw-login@test.com";
        String password = "password1!";

        String accessToken = createUserAndSignIn(
                email,
                "Withdraw User",
                password,
                UserRole.USER
        );

        UUID userId = jwtProvider.parseAccessToken(accessToken).userId();

        mockMvc.perform(delete("/api/users/{userId}", userId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        // when & then
        mockMvc.perform(post("/api/auth/sign-in")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                            {
                              "email": "%s",
                              "password": "%s"
                            }
                            """.formatted(email, password)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIAL"));
    }

    @Test
    @DisplayName("다른 사용자의 계정은 탈퇴할 수 없다")
    void withdrawUser_forbidden() throws Exception {
        // given
        User targetUser = userRepository.save(new User(
                "withdraw-target@test.com",
                "Target User",
                passwordEncoder.encode("password1!"),
                null,
                UserRole.USER
        ));

        String accessToken = createUserAndSignIn(
                "withdraw-other@test.com",
                "Other User",
                "password1!",
                UserRole.USER
        );

        // when & then
        mockMvc.perform(delete("/api/users/{userId}", targetUser.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        User unchangedUser = userRepository.findById(targetUser.getId()).orElseThrow();
        assertThat(unchangedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
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
    }

    @Test
    @DisplayName("CSRF 토큰 없이 계정을 탈퇴할 수 없다")
    void withdrawUser_withoutCsrf() throws Exception {
        // given
        String accessToken = createUserAndSignIn(
                "withdraw-csrf@test.com",
                "Withdraw User",
                "password1!",
                UserRole.USER
        );

        UUID userId = jwtProvider.parseAccessToken(accessToken).userId();

        // when & then
        mockMvc.perform(delete("/api/users/{userId}", userId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isForbidden());
    }

    private String createUserAndSignIn(
            String email,
            String name,
            String password,
            UserRole role
    ) throws Exception {
        userRepository.save(new User(
                email,
                name,
                passwordEncoder.encode(password),
                null,
                role
        ));

        return signIn(email, password);
    }

    private String signIn(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/sign-in")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        String accessToken = jsonNode.get("accessToken").asText();

        JwtClaims claims = jwtProvider.parseAccessToken(accessToken);
        assertThat(claims.sessionId()).isNotNull();

        return accessToken;
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }
}
