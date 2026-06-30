package com.example.sb10_MoPl_team3.user.repository;

import com.example.sb10_MoPl_team3.global.config.JpaAuditingConfig;
import com.example.sb10_MoPl_team3.global.config.QuerydslConfig;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.user.dto.request.UserSearchCondition;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.enums.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({QuerydslConfig.class, JpaAuditingConfig.class})
class UserRepositoryImplTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("이메일, 권한, 잠금 상태 조건으로 사용자를 검색한다")
    void searchUsers_filter() {
        // given
        User activeUser = saveUser("alpha@test.com", "Alpha", UserRole.USER, UserStatus.ACTIVE);
        saveUser("beta@test.com", "Beta", UserRole.ADMIN, UserStatus.ACTIVE);
        saveUser("gamma@test.com", "Gamma", UserRole.USER, UserStatus.LOCKED);

        UserSearchCondition condition = new UserSearchCondition(
                "alpha",
                UserRole.USER,
                false,
                null,
                null,
                20,
                "ASCENDING",
                "email"
        );

        // when
        List<User> result = userRepository.searchUsers(condition, 21);
        long totalCount = userRepository.countUsers(condition);

        // then
        assertThat(result).containsExactly(activeUser);
        assertThat(totalCount).isEqualTo(1L);
    }

    @Test
    @DisplayName("정렬 기준과 방향에 따라 사용자 목록을 정렬한다")
    void searchUsers_sort() {
        // given
        User bUser = saveUser("b@test.com", "B User", UserRole.USER, UserStatus.ACTIVE);
        User aUser = saveUser("a@test.com", "A User", UserRole.USER, UserStatus.ACTIVE);

        UserSearchCondition condition = new UserSearchCondition(
                null,
                null,
                null,
                null,
                null,
                20,
                "ASCENDING",
                "email"
        );

        // when
        List<User> result = userRepository.searchUsers(condition, 21);

        // then
        assertThat(result).containsExactly(aUser, bUser);
    }

    @Test
    @DisplayName("커서와 보조 커서 이후의 사용자 목록을 조회한다")
    void searchUsers_cursor() {
        // given
        saveUser("a@test.com", "A User", UserRole.USER, UserStatus.ACTIVE);
        User bUser = saveUser("b@test.com", "B User", UserRole.USER, UserStatus.ACTIVE);
        User cUser = saveUser("c@test.com", "C User", UserRole.USER, UserStatus.ACTIVE);

        UserSearchCondition condition = new UserSearchCondition(
                null,
                null,
                null,
                "b@test.com",
                bUser.getId(),
                20,
                "ASCENDING",
                "email"
        );

        // when
        List<User> result = userRepository.searchUsers(condition, 21);

        // then
        assertThat(result).containsExactly(cUser);
    }

    @Test
    @DisplayName("countUsers는 커서 조건과 무관하게 필터 기준 전체 개수를 반환한다")
    void countUsers_ignoreCursor() {
        // given
        saveUser("a@test.com", "A User", UserRole.USER, UserStatus.ACTIVE);
        User bUser = saveUser("b@test.com", "B User", UserRole.USER, UserStatus.ACTIVE);
        saveUser("c@test.com", "C User", UserRole.USER, UserStatus.ACTIVE);

        UserSearchCondition condition = new UserSearchCondition(
                null,
                UserRole.USER,
                false,
                "b@test.com",
                bUser.getId(),
                20,
                "ASCENDING",
                "email"
        );

        // when
        long totalCount = userRepository.countUsers(condition);

        // then
        assertThat(totalCount).isEqualTo(3L);
    }

    @Test
    @DisplayName("커서와 보조 커서 중 하나만 있으면 예외가 발생한다")
    void searchUsers_invalidCursor() {
        // given
        UserSearchCondition condition = new UserSearchCondition(
                null,
                null,
                null,
                "cursor-only",
                null,
                20,
                "ASCENDING",
                "email"
        );

        // when & then
        assertThatThrownBy(() -> userRepository.searchUsers(condition, 21))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CURSOR);
    }

    private User saveUser(
            String email,
            String name,
            UserRole role,
            UserStatus status
    ) {
        User user = new User(
                email,
                name,
                "encoded-password",
                null,
                role
        );

        ReflectionTestUtils.setField(user, "status", status);

        return userRepository.saveAndFlush(user);
    }
}