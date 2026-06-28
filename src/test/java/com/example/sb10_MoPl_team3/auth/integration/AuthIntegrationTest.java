package com.example.sb10_MoPl_team3.auth.integration;

import com.example.sb10_MoPl_team3.auth.entity.AuthSession;
import com.example.sb10_MoPl_team3.auth.repository.AuthSessionRepository;
import com.example.sb10_MoPl_team3.auth.service.TokenService;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtClaims;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtProvider;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtTokenType;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.enums.UserStatus;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpHeaders.SET_COOKIE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Testcontainers(disabledWithoutDocker = true)
class AuthIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthSessionRepository authSessionRepository;

    @Autowired
    private TokenService tokenService;

    @AfterEach
    void cleanRedis() {
        authSessionRepository.deleteAll();
    }

    @Test
    @DisplayName("회원가입 요청이 유효하면 사용자를 저장하고 비밀번호를 암호화한다")
    void signUp_success() throws Exception {
        signUp("Test User", "user@test.com", "password1!")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("user@test.com"))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.locked").value(false));

        User savedUser = userRepository.findByEmail("user@test.com").orElseThrow();

        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);
        assertThat(savedUser.getPassword()).isNotEqualTo("password1!");
        assertThat(passwordEncoder.matches("password1!", savedUser.getPassword())).isTrue();
    }

    @Test
    @DisplayName("이미 가입된 이메일로 회원가입하면 409를 반환한다")
    void signUp_duplicateEmail() throws Exception {
        signUp("Test User", "duplicate@test.com", "password1!")
                .andExpect(status().isCreated());

        signUp("Other User", "duplicate@test.com", "password1!")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("회원가입한 계정은 이메일과 비밀번호로 로그인할 수 있고 Access Token을 발급받는다")
    void signIn_success() throws Exception {
        signUp("Test User", "login@test.com", "password1!")
                .andExpect(status().isCreated());

        MvcResult result = signIn("login@test.com", "password1!")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.userDto.email").value("login@test.com"))
                .andExpect(jsonPath("$.userDto.role").value("USER"))
                .andExpect(jsonPath("$.userDto.locked").value(false))
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        String accessToken = jsonNode.get("accessToken").asText();
        Cookie refreshTokenCookie = result.getResponse().getCookie("REFRESH_TOKEN");
        assertThat(refreshTokenCookie).isNotNull();
        assertThat(refreshTokenCookie.isHttpOnly()).isTrue();
        assertThat(refreshTokenCookie.getPath()).isEqualTo("/api/auth");

        String refreshToken = refreshTokenCookie.getValue();
        String expectedRefreshTokenHash = tokenService.hashRefreshToken(refreshToken);

        JwtClaims claims = jwtProvider.parseAccessToken(accessToken);
        User savedUser = userRepository.findByEmail("login@test.com").orElseThrow();
        AuthSession savedSession = authSessionRepository.findById(claims.sessionId()).orElseThrow();

        assertThat(claims.userId()).isEqualTo(savedUser.getId());
        assertThat(claims.role()).isEqualTo(UserRole.USER);
        assertThat(claims.type()).isEqualTo(JwtTokenType.ACCESS);
        assertThat(claims.sessionId()).isNotNull();

        assertThat(savedSession.getUserId()).isEqualTo(savedUser.getId());
        assertThat(savedSession.getRefreshTokenHash()).isEqualTo(expectedRefreshTokenHash);
        assertThat(authSessionRepository.findByRefreshTokenHash(expectedRefreshTokenHash))
                .hasValueSatisfying(session -> assertThat(session.getId()).isEqualTo(savedSession.getId()));
        assertThat(savedSession.getExpiresAt()).isAfter(savedSession.getCreatedAt());
        assertThat(savedSession.isRevoked()).isFalse();
        assertThat(savedSession.getRevokedAt()).isNull();
        assertThat(savedSession.getTtlSeconds()).isPositive();
    }

    @Test
    @DisplayName("잠긴 계정은 로그인할 수 없다")
    void signIn_lockedUser() throws Exception {
        User user = new User(
                "locked@test.com",
                "Locked User",
                passwordEncoder.encode("password1!"),
                null,
                UserRole.USER
        );
        ReflectionTestUtils.setField(user, "status", UserStatus.LOCKED);
        userRepository.save(user);

        signIn("locked@test.com", "password1!")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIAL"));
    }

    @Test
    @DisplayName("CSRF 토큰 없이 회원가입을 요청하면 403을 반환한다")
    void signUp_withoutCsrf() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Test User",
                                  "email": "csrf@test.com",
                                  "password": "password1!"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CSRF 토큰 없이 로그인을 요청하면 403을 반환한다")
    void signIn_withoutCsrf() throws Exception {
        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(APPLICATION_JSON)
                        .content("""
                            {
                              "email": "user@test.com",
                              "password": "password1!"
                            }
                            """))
                .andExpect(status().isForbidden());
    }

    private ResultActions signUp(String name, String email, String password) throws Exception {
        return mockMvc.perform(post("/api/users")
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content("""
                        {
                          "name": "%s",
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(name, email, password)));
    }

    private ResultActions signIn(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/sign-in")
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content("""
                        {
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(email, password)));
    }

    @Test
    @DisplayName("refresh token 쿠키가 유효하면 새 access token과 새 refresh token 쿠키를 발급한다")
    void refresh_success() throws Exception {
        signUp("Test User", "refresh@test.com", "password1!")
                .andExpect(status().isCreated());

        MvcResult signInResult = signIn("refresh@test.com", "password1!")
                .andExpect(status().isOk())
                .andReturn();

        Cookie refreshTokenCookie = signInResult.getResponse().getCookie("REFRESH_TOKEN");
        assertThat(refreshTokenCookie).isNotNull();

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .with(csrf())
                        .cookie(refreshTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.userDto.email").value("refresh@test.com"))
                .andReturn();

        Cookie newRefreshTokenCookie = refreshResult.getResponse().getCookie("REFRESH_TOKEN");
        assertThat(newRefreshTokenCookie).isNotNull();
        assertThat(newRefreshTokenCookie.getValue()).isNotBlank();
        assertThat(newRefreshTokenCookie.isHttpOnly()).isTrue();
        assertThat(newRefreshTokenCookie.getPath()).isEqualTo("/api/auth");

        JsonNode jsonNode = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String newAccessToken = jsonNode.get("accessToken").asText();

        JwtClaims claims = jwtProvider.parseAccessToken(newAccessToken);
        User savedUser = userRepository.findByEmail("refresh@test.com").orElseThrow();

        assertThat(claims.userId()).isEqualTo(savedUser.getId());
        assertThat(claims.sessionId()).isNotNull();

        String newRefreshTokenHash = tokenService.hashRefreshToken(newRefreshTokenCookie.getValue());
        assertThat(authSessionRepository.findByRefreshTokenHash(newRefreshTokenHash)).isPresent();
    }

    @Test
    @DisplayName("refresh token 쿠키가 없으면 401을 반환한다")
    void refresh_missingCookie() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIAL"));
    }

    @Test
    @DisplayName("로그아웃하면 현재 인증 세션을 무효화하고 refresh token 쿠키를 만료한다")
    void signOut_success() throws Exception {
        signUp("Test User", "logout@test.com", "password1!")
                .andExpect(status().isCreated());

        MvcResult signInResult = signIn("logout@test.com", "password1!")
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(signInResult.getResponse().getContentAsString());
        String accessToken = jsonNode.get("accessToken").asText();

        JwtClaims claims = jwtProvider.parseAccessToken(accessToken);

        mockMvc.perform(post("/api/auth/sign-out")
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent())
                .andExpect(header().string(SET_COOKIE, containsString("REFRESH_TOKEN=")))
                .andExpect(header().string(SET_COOKIE, containsString("Max-Age=0")));

        AuthSession authSession = authSessionRepository.findById(claims.sessionId()).orElseThrow();

        assertThat(authSession.isRevoked()).isTrue();
        assertThat(authSession.getRevokedAt()).isNotNull();
    }
}
