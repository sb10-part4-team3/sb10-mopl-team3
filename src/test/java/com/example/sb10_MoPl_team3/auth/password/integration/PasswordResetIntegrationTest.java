package com.example.sb10_MoPl_team3.auth.password.integration;

import com.example.sb10_MoPl_team3.auth.password.entity.PasswordResetToken;
import com.example.sb10_MoPl_team3.auth.password.repository.PasswordResetTokenRepository;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PasswordResetIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private Clock clock;

    @Test
    @DisplayName("임시 비밀번호 발급 요청이 유효하면 3분 만료 토큰을 저장한다")
    void issueTemporaryPassword_success() throws Exception {
        Instant now = Instant.parse("2026-06-28T00:00:00Z");
        given(clock.instant()).willReturn(now);

        User user = userRepository.save(new User(
                "reset@test.com",
                "Reset User",
                passwordEncoder.encode("password1!"),
                null,
                UserRole.USER
        ));

        mockMvc.perform(post("/api/auth/password-reset")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "reset@test.com"
                                }
                                """))
                .andExpect(status().isNoContent());

        List<PasswordResetToken> tokens =
                passwordResetTokenRepository.findAllByUser_IdAndUsedFalse(user.getId());

        assertThat(tokens).hasSize(1);

        PasswordResetToken token = tokens.get(0);

        assertThat(token.getUser().getId()).isEqualTo(user.getId());
        assertThat(token.getTemporaryPassword()).isNotBlank();
        assertThat(token.getTemporaryPassword()).isNotEqualTo("temporary1!!");
        assertThat(token.getExpiresAt()).isEqualTo(now.plus(Duration.ofMinutes(3)));
        assertThat(token.isUsed()).isFalse();
        assertThat(token.getUsedAt()).isNull();
    }

    @Test
    @DisplayName("기존 미사용 임시 비밀번호가 있으면 사용 처리하고 새 토큰을 저장한다")
    void issueTemporaryPassword_marksExistingTokenUsed() throws Exception {
        Instant now = Instant.parse("2026-06-28T00:00:00Z");
        given(clock.instant()).willReturn(now);

        User user = userRepository.save(new User(
                "reset-existing@test.com",
                "Reset User",
                passwordEncoder.encode("password1!"),
                null,
                UserRole.USER
        ));

        PasswordResetToken existingToken = passwordResetTokenRepository.save(
                PasswordResetToken.create(
                        user,
                        passwordEncoder.encode("old-temporary1!!"),
                        now.plus(Duration.ofMinutes(1)),
                        now.minus(Duration.ofMinutes(1))
                )
        );

        mockMvc.perform(post("/api/auth/password-reset")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "reset-existing@test.com"
                                }
                                """))
                .andExpect(status().isNoContent());

        PasswordResetToken usedToken =
                passwordResetTokenRepository.findById(existingToken.getId()).orElseThrow();
        List<PasswordResetToken> usableTokens =
                passwordResetTokenRepository.findAllByUser_IdAndUsedFalse(user.getId());

        assertThat(usedToken.isUsed()).isTrue();
        assertThat(usedToken.getUsedAt()).isEqualTo(now);
        assertThat(usableTokens).hasSize(1);
        assertThat(usableTokens.get(0).getId()).isNotEqualTo(existingToken.getId());
        assertThat(usableTokens.get(0).getTemporaryPassword()).isNotBlank();
        assertThat(usableTokens.get(0).getTemporaryPassword()).isNotEqualTo("temporary1!!");
    }

    @Test
    @DisplayName("존재하지 않는 이메일이어도 204를 반환하고 임시 비밀번호를 저장하지 않는다")
    void issueTemporaryPassword_unknownEmail() throws Exception {
        mockMvc.perform(post("/api/auth/password-reset")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "unknown@test.com"
                                }
                                """))
                .andExpect(status().isNoContent());

        assertThat(passwordResetTokenRepository.count()).isZero();
    }
}
