package com.example.sb10_MoPl_team3.global.security.jwt;

import com.example.sb10_MoPl_team3.auth.entity.AuthSession;
import org.springframework.test.util.ReflectionTestUtils;
import com.example.sb10_MoPl_team3.auth.repository.AuthSessionRepository;
import com.example.sb10_MoPl_team3.global.security.AuthUser;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(JwtAuthenticationIntegrationTest.ProtectedTestController.class)
class JwtAuthenticationIntegrationTest {

    private static final String PROTECTED_URI = "/api/test/protected/me";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private AuthSessionRepository authSessionRepository;

    @Test
    @DisplayName("로그인으로 발급받은 Access Token으로 보호 API를 호출할 수 있다")
    void accessProtectedApiWithAccessToken() throws Exception {
        User user = userRepository.save(new User(
                "jwt-user@test.com",
                "Jwt User",
                passwordEncoder.encode("password1!"),
                null,
                UserRole.USER
        ));

        String accessToken = signInAndExtractAccessToken("jwt-user@test.com", "password1!");

        JwtClaims claims = jwtProvider.parseAccessToken(accessToken);
        AuthSession authSession = createAuthSession(
                claims.sessionId(),
                user.getId(),
                Instant.now().plus(Duration.ofDays(7))
        );

        given(authSessionRepository.findById(claims.sessionId()))
                .willReturn(Optional.of(authSession));

        mockMvc.perform(get(PROTECTED_URI)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId().toString()))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("Access Token 없이 보호 API를 호출하면 401을 반환한다")
    void accessProtectedApiWithoutAccessToken() throws Exception {
        mockMvc.perform(get(PROTECTED_URI))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("세션을 찾을 수 없는 Access Token으로 보호 API를 호출하면 401을 반환한다")
    void accessProtectedApiWithMissingSession() throws Exception {
        userRepository.save(new User(
                "jwt-missing-session@test.com",
                "Jwt User",
                passwordEncoder.encode("password1!"),
                null,
                UserRole.USER
        ));

        String accessToken = signInAndExtractAccessToken("jwt-missing-session@test.com", "password1!");
        JwtClaims claims = jwtProvider.parseAccessToken(accessToken);

        given(authSessionRepository.findById(claims.sessionId()))
                .willReturn(Optional.empty());

        mockMvc.perform(get(PROTECTED_URI)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("유효하지 않은 Access Token으로 보호 API를 호출하면 401을 반환한다")
    void accessProtectedApiWithInvalidAccessToken() throws Exception {
        mockMvc.perform(get(PROTECTED_URI)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("무효화된 세션의 Access Token으로 보호 API를 호출하면 401을 반환한다")
    void accessProtectedApiWithRevokedSession() throws Exception {
        User user = userRepository.save(new User(
                "jwt-revoked@test.com",
                "Jwt User",
                passwordEncoder.encode("password1!"),
                null,
                UserRole.USER
        ));

        String accessToken = signInAndExtractAccessToken("jwt-revoked@test.com", "password1!");
        JwtClaims claims = jwtProvider.parseAccessToken(accessToken);

        AuthSession authSession = createAuthSession(
                claims.sessionId(),
                user.getId(),
                Instant.now().plus(Duration.ofDays(7))
        );
        authSession.revoke(Instant.now());

        given(authSessionRepository.findById(claims.sessionId()))
                .willReturn(Optional.of(authSession));

        mockMvc.perform(get(PROTECTED_URI)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("토큰 사용자와 세션 사용자가 다르면 401을 반환한다")
    void accessProtectedApiWithSessionUserMismatch() throws Exception {
        userRepository.save(new User(
                "jwt-mismatch@test.com",
                "Jwt User",
                passwordEncoder.encode("password1!"),
                null,
                UserRole.USER
        ));

        String accessToken = signInAndExtractAccessToken("jwt-mismatch@test.com", "password1!");
        JwtClaims claims = jwtProvider.parseAccessToken(accessToken);

        AuthSession authSession = createAuthSession(
                claims.sessionId(),
                UUID.randomUUID(),
                Instant.now().plus(Duration.ofDays(7))
        );

        given(authSessionRepository.findById(claims.sessionId()))
                .willReturn(Optional.of(authSession));

        mockMvc.perform(get(PROTECTED_URI)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    private String signInAndExtractAccessToken(String email, String password) throws Exception {
        String responseBody = mockMvc.perform(post("/api/auth/sign-in")
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
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("REFRESH_TOKEN=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(responseBody);

        return jsonNode.get("accessToken").asText();
    }

    @RestController
    static class ProtectedTestController {

        @GetMapping(PROTECTED_URI)
        Map<String, String> me(Authentication authentication) {
            AuthUser authUser = (AuthUser) authentication.getPrincipal();

            return Map.of(
                    "userId", authUser.userId().toString(),
                    "role", authUser.role().name()
            );
        }
    }

    private AuthSession createAuthSession(UUID sessionId, UUID userId, Instant expiresAt) {
        Instant now = Instant.now();

        AuthSession authSession = AuthSession.create(
                userId,
                "refresh-token-hash",
                expiresAt,
                now
        );

        ReflectionTestUtils.setField(authSession, "id", sessionId);

        return authSession;
    }
}
