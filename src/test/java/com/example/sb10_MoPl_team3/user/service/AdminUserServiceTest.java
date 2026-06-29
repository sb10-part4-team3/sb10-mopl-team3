package com.example.sb10_MoPl_team3.user.service;

import com.example.sb10_MoPl_team3.auth.entity.AuthSession;
import com.example.sb10_MoPl_team3.auth.repository.AuthSessionRepository;
import com.example.sb10_MoPl_team3.global.cursor.CursorPageRequest;
import com.example.sb10_MoPl_team3.global.cursor.CursorResponse;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.user.dto.request.UserRoleUpdateRequest;
import com.example.sb10_MoPl_team3.user.dto.request.UserSearchCondition;
import com.example.sb10_MoPl_team3.user.dto.response.UserDto;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthSessionRepository authSessionRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private AdminUserService adminUserService;

    @Test
    @DisplayName("사용자 목록을 조건에 따라 조회하고 커서 페이지 응답으로 반환한다")
    void findUsers_success() {
        // given
        UserSearchCondition condition = new UserSearchCondition(
                "test",
                UserRole.USER,
                false,
                null,
                null,
                2,
                "ASCENDING",
                "email"
        );

        UUID user1Id = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();
        UUID user3Id = UUID.randomUUID();

        User user1 = createUser(user1Id, "a@test.com", "A User", UserRole.USER, "2026-06-28T00:00:00Z");
        User user2 = createUser(user2Id, "b@test.com", "B User", UserRole.USER, "2026-06-28T00:01:00Z");
        User user3 = createUser(user3Id, "c@test.com", "C User", UserRole.USER, "2026-06-28T00:02:00Z");

        given(userRepository.searchUsers(condition, 3))
                .willReturn(List.of(user1, user2, user3));

        given(userRepository.countUsers(condition))
                .willReturn(3L);

        // when
        CursorResponse<UserDto> response = adminUserService.findUsers(condition);

        // then
        assertThat(response.data()).hasSize(2);
        assertThat(response.data().get(0).email()).isEqualTo("a@test.com");
        assertThat(response.data().get(1).email()).isEqualTo("b@test.com");

        assertThat(response.hasNext()).isTrue();
        assertThat(response.totalCount()).isEqualTo(3L);
        assertThat(response.sortBy()).isEqualTo("email");
        assertThat(response.sortDirection()).isEqualTo("ASCENDING");
        assertThat(response.nextCursor()).isEqualTo("b@test.com");
        assertThat(response.nextIdAfter()).isEqualTo(user2Id);

        then(userRepository).should().searchUsers(condition, 3);
        then(userRepository).should().countUsers(condition);
    }

    @Test
    @DisplayName("다음 페이지가 없으면 hasNext는 false이고 다음 커서는 null이다")
    void findUsers_noNextPage() {
        // given
        UserSearchCondition condition = new UserSearchCondition(
                null,
                null,
                null,
                null,
                null,
                10,
                "DESCENDING",
                "createdAt"
        );

        UUID userId = UUID.randomUUID();
        User user = createUser(userId, "user@test.com", "User", UserRole.USER, "2026-06-28T00:00:00Z");

        given(userRepository.searchUsers(condition, 11))
                .willReturn(List.of(user));

        given(userRepository.countUsers(condition))
                .willReturn(1L);

        // when
        CursorResponse<UserDto> response = adminUserService.findUsers(condition);

        // then
        assertThat(response.data()).hasSize(1);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
        assertThat(response.nextIdAfter()).isNull();
        assertThat(response.totalCount()).isEqualTo(1L);
        assertThat(response.sortBy()).isEqualTo("createdAt");
        assertThat(response.sortDirection()).isEqualTo("DESCENDING");
    }

    @Test
    @DisplayName("limit이 0 이하이면 기본 크기보다 하나 더 많은 개수를 조회한다")
    void findUsers_defaultLimit() {
        // given
        UserSearchCondition condition = new UserSearchCondition(
                null,
                null,
                null,
                null,
                null,
                0,
                "DESCENDING",
                "createdAt"
        );

        given(userRepository.searchUsers(condition, CursorPageRequest.DEFAULT_SIZE + 1))
                .willReturn(List.of());

        given(userRepository.countUsers(condition))
                .willReturn(0L);

        // when
        adminUserService.findUsers(condition);

        // then
        ArgumentCaptor<Integer> limitCaptor = ArgumentCaptor.forClass(Integer.class);
        then(userRepository).should().searchUsers(eq(condition), limitCaptor.capture());

        assertThat(limitCaptor.getValue()).isEqualTo(CursorPageRequest.DEFAULT_SIZE + 1);
    }

    @Test
    @DisplayName("limit이 최대 크기를 넘으면 최대 크기보다 하나 더 많은 개수를 조회한다")
    void findUsers_maxLimit() {
        // given
        UserSearchCondition condition = new UserSearchCondition(
                null,
                null,
                null,
                null,
                null,
                CursorPageRequest.MAX_SIZE + 50,
                "DESCENDING",
                "createdAt"
        );

        given(userRepository.searchUsers(condition, CursorPageRequest.MAX_SIZE + 1))
                .willReturn(List.of());

        given(userRepository.countUsers(condition))
                .willReturn(0L);

        // when
        adminUserService.findUsers(condition);

        // then
        then(userRepository).should().searchUsers(condition, CursorPageRequest.MAX_SIZE + 1);
    }

    @Test
    @DisplayName("허용되지 않은 정렬 기준이면 예외가 발생한다")
    void findUsers_invalidSortBy() {
        // given
        UserSearchCondition condition = new UserSearchCondition(
                null,
                null,
                null,
                null,
                null,
                20,
                "DESCENDING",
                "invalid"
        );

        // when & then
        assertThatThrownBy(() -> adminUserService.findUsers(condition))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("관리자는 사용자의 권한을 변경하고 해당 사용자의 세션을 모두 무효화한다")
    void updateUserRole_success() {
        // given
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-29T00:00:00Z");

        User user = createUser(
                userId,
                "user@test.com",
                "User",
                UserRole.USER,
                "2026-06-28T00:00:00Z"
        );

        AuthSession session1 = AuthSession.create(
                userId,
                "refresh-token-hash-1",
                now.plusSeconds(3600),
                now
        );

        AuthSession session2 = AuthSession.create(
                userId,
                "refresh-token-hash-2",
                now.plusSeconds(3600),
                now
        );

        UserRoleUpdateRequest request = new UserRoleUpdateRequest(UserRole.ADMIN);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(authSessionRepository.findAllByUserId(userId)).willReturn(List.of(session1, session2));
        given(clock.instant()).willReturn(now);

        // when
        UserDto response = adminUserService.updateUserRole(userId, request);

        // then
        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(response.role()).isEqualTo(UserRole.ADMIN);

        assertThat(session1.isRevoked()).isTrue();
        assertThat(session1.getRevokedAt()).isEqualTo(now);
        assertThat(session2.isRevoked()).isTrue();
        assertThat(session2.getRevokedAt()).isEqualTo(now);

        then(userRepository).should().findById(userId);
        then(authSessionRepository).should().findAllByUserId(userId);
        then(authSessionRepository).should().saveAll(List.of(session1, session2));
    }

    @Test
    @DisplayName("권한을 변경할 사용자가 없으면 예외가 발생한다")
    void updateUserRole_userNotFound() {
        // given
        UUID userId = UUID.randomUUID();
        UserRoleUpdateRequest request = new UserRoleUpdateRequest(UserRole.ADMIN);

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminUserService.updateUserRole(userId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    private User createUser(
            UUID id,
            String email,
            String name,
            UserRole role,
            String createdAt
    ) {
        User user = new User(
                email,
                name,
                "encoded-password",
                null,
                role
        );

        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "createdAt", Instant.parse(createdAt));

        return user;
    }
}