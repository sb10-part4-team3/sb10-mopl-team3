package com.example.sb10_MoPl_team3.user.integration;

import com.example.sb10_MoPl_team3.auth.entity.AuthSession;
import com.example.sb10_MoPl_team3.auth.repository.AuthSessionRepository;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtClaims;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtProvider;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.enums.UserStatus;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Testcontainers(disabledWithoutDocker = true)
class AdminUserIntegrationTest {

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
    @DisplayName("관리자는 사용자 목록을 조회할 수 있다")
    void findUsers_admin_success() throws Exception {
        String adminAccessToken = signInAdmin();

        userRepository.save(new User(
                "user1@test.com",
                "User A",
                passwordEncoder.encode("password1!"),
                null,
                UserRole.USER
        ));

        userRepository.save(new User(
                "user2@test.com",
                "User B",
                passwordEncoder.encode("password1!"),
                null,
                UserRole.USER
        ));

        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminAccessToken))
                        .param("limit", "20")
                        .param("sortBy", "email")
                        .param("sortDirection", "ASCENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].email").exists())
                .andExpect(jsonPath("$.totalCount").value(3))
                .andExpect(jsonPath("$.sortBy").value("email"))
                .andExpect(jsonPath("$.sortDirection").value("ASCENDING"));
    }

    @Test
    @DisplayName("일반 사용자는 사용자 목록을 조회할 수 없다")
    void findUsers_user_forbidden() throws Exception {
        String userAccessToken = createUserAndSignIn(
                "user@test.com",
                "User",
                "password1!",
                UserRole.USER
        );

        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAccessToken))
                        .param("limit", "20")
                        .param("sortBy", "email")
                        .param("sortDirection", "ASCENDING"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("관리자는 사용자 권한을 변경하고 해당 사용자의 세션을 무효화할 수 있다")
    void updateUserRole_admin_success() throws Exception {
        String adminAccessToken = signInAdmin();

        User targetUser = userRepository.save(new User(
                "target-role@test.com",
                "Target",
                passwordEncoder.encode("password1!"),
                null,
                UserRole.USER
        ));

        String targetAccessToken = signIn("target-role@test.com", "password1!");
        UUID targetSessionId = jwtProvider.parseAccessToken(targetAccessToken).sessionId();

        mockMvc.perform(patch("/api/users/{userId}/role", targetUser.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminAccessToken))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(targetUser.getId().toString()))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        User updatedUser = userRepository.findById(targetUser.getId()).orElseThrow();
        AuthSession revokedSession = authSessionRepository.findById(targetSessionId).orElseThrow();

        assertThat(updatedUser.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(revokedSession.isRevoked()).isTrue();
        assertThat(revokedSession.getRevokedAt()).isNotNull();
    }

    @Test
    @DisplayName("일반 사용자는 사용자 권한을 변경할 수 없다")
    void updateUserRole_user_forbidden() throws Exception {
        String userAccessToken = createUserAndSignIn(
                "role-user@test.com",
                "User",
                "password1!",
                UserRole.USER
        );

        User targetUser = userRepository.save(new User(
                "role-target@test.com",
                "Target",
                passwordEncoder.encode("password1!"),
                null,
                UserRole.USER
        ));

        mockMvc.perform(patch("/api/users/{userId}/role", targetUser.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAccessToken))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isForbidden());

        User unchangedUser = userRepository.findById(targetUser.getId()).orElseThrow();

        assertThat(unchangedUser.getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("관리자는 사용자 계정을 잠그고 해당 사용자의 세션을 무효화할 수 있다")
    void updateUserLocked_admin_success() throws Exception {
        String adminAccessToken = signInAdmin();

        User targetUser = userRepository.save(new User(
                "target-lock@test.com",
                "Target",
                passwordEncoder.encode("password1!"),
                null,
                UserRole.USER
        ));

        String targetAccessToken = signIn("target-lock@test.com", "password1!");
        UUID targetSessionId = jwtProvider.parseAccessToken(targetAccessToken).sessionId();

        mockMvc.perform(patch("/api/users/{userId}/locked", targetUser.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminAccessToken))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "locked": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(targetUser.getId().toString()))
                .andExpect(jsonPath("$.locked").value(true));

        User updatedUser = userRepository.findById(targetUser.getId()).orElseThrow();
        AuthSession revokedSession = authSessionRepository.findById(targetSessionId).orElseThrow();

        assertThat(updatedUser.getStatus()).isEqualTo(UserStatus.LOCKED);
        assertThat(revokedSession.isRevoked()).isTrue();
        assertThat(revokedSession.getRevokedAt()).isNotNull();
    }

    @Test
    @DisplayName("관리자는 사용자 계정 잠금을 해제할 수 있다")
    void updateUserUnlocked_admin_success() throws Exception {
        String adminAccessToken = signInAdmin();

        User targetUser = userRepository.save(new User(
                "target-unlock@test.com",
                "Target",
                passwordEncoder.encode("password1!"),
                null,
                UserRole.USER
        ));
        targetUser.changeStatus(UserStatus.LOCKED);

        mockMvc.perform(patch("/api/users/{userId}/locked", targetUser.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminAccessToken))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "locked": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(targetUser.getId().toString()))
                .andExpect(jsonPath("$.locked").value(false));

        User updatedUser = userRepository.findById(targetUser.getId()).orElseThrow();

        assertThat(updatedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("일반 사용자는 사용자 계정 잠금 상태를 변경할 수 없다")
    void updateUserLocked_user_forbidden() throws Exception {
        String userAccessToken = createUserAndSignIn(
                "lock-user@test.com",
                "User",
                "password1!",
                UserRole.USER
        );

        User targetUser = userRepository.save(new User(
                "lock-target@test.com",
                "Target",
                passwordEncoder.encode("password1!"),
                null,
                UserRole.USER
        ));

        mockMvc.perform(patch("/api/users/{userId}/locked", targetUser.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(userAccessToken))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "locked": true
                                }
                                """))
                .andExpect(status().isForbidden());

        User unchangedUser = userRepository.findById(targetUser.getId()).orElseThrow();

        assertThat(unchangedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
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

    private String signInAdmin() throws Exception {
        return signIn("admin@test.com", "adminPassword1!");
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
