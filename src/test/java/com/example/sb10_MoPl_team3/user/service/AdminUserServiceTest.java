package com.example.sb10_MoPl_team3.user.service;

import com.example.sb10_MoPl_team3.global.cursor.CursorPageRequest;
import com.example.sb10_MoPl_team3.global.cursor.CursorResponse;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
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

import java.time.Instant;
import java.util.List;
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