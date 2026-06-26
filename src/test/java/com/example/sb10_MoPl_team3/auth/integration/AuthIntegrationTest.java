package com.example.sb10_MoPl_team3.auth.integration;

import com.example.sb10_MoPl_team3.auth.repository.AuthSessionRepository;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtClaims;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtProvider;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtTokenType;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.enums.UserStatus;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthIntegrationTest {

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

    @MockitoBean
    private AuthSessionRepository authSessionRepository;

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
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.userDto.email").value("login@test.com"))
                .andExpect(jsonPath("$.userDto.role").value("USER"))
                .andExpect(jsonPath("$.userDto.locked").value(false))
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        String accessToken = jsonNode.get("accessToken").asText();

        JwtClaims claims = jwtProvider.parseAccessToken(accessToken);
        User savedUser = userRepository.findByEmail("login@test.com").orElseThrow();

        assertThat(claims.userId()).isEqualTo(savedUser.getId());
        assertThat(claims.role()).isEqualTo(UserRole.USER);
        assertThat(claims.type()).isEqualTo(JwtTokenType.ACCESS);
        assertThat(claims.sessionId()).isNotNull();
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
}
