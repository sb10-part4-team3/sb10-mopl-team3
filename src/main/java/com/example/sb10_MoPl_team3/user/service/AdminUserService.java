package com.example.sb10_MoPl_team3.user.service;

import com.example.sb10_MoPl_team3.auth.entity.AuthSession;
import com.example.sb10_MoPl_team3.auth.repository.AuthSessionRepository;
import com.example.sb10_MoPl_team3.auth.service.AuthSessionLockManager;
import com.example.sb10_MoPl_team3.global.cursor.CursorPageRequest;
import com.example.sb10_MoPl_team3.global.cursor.CursorResponse;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.user.config.AdminAccountProperties;
import com.example.sb10_MoPl_team3.user.dto.request.UserLockUpdateRequest;
import com.example.sb10_MoPl_team3.user.dto.request.UserRoleUpdateRequest;
import com.example.sb10_MoPl_team3.user.dto.request.UserSearchCondition;
import com.example.sb10_MoPl_team3.user.dto.response.UserDto;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserStatus;
import com.example.sb10_MoPl_team3.user.mapper.UserMapper;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import com.example.sb10_MoPl_team3.notification.enums.NotificationLevel;
import com.example.sb10_MoPl_team3.notification.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private static final String DEFAULT_SORT_BY = "createdAt";
    private static final String DEFAULT_SORT_DIRECTION = "DESCENDING";
    private static final Set<String> ALLOWED_SORT_BY = Set.of(
            "name",
            "email",
            "createdAt",
            "isLocked",
            "role"
    );

    private final UserRepository userRepository;
    private final AuthSessionRepository authSessionRepository;
    private final AuthSessionLockManager authSessionLockManager;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;
    private final AdminAccountProperties adminAccountProperties;

    public CursorResponse<UserDto> findUsers(UserSearchCondition condition) {
        String sortBy = normalizeSortBy(condition.sortBy());
        String sortDirection = normalizeSortDirection(condition.sortDirection());
        int limit = normalizeLimit(condition.limit());

        UserSearchCondition normalizedCondition = new UserSearchCondition(
                condition.keyword(),
                condition.roleEqual(),
                condition.isLocked(),
                condition.cursor(),
                condition.idAfter(),
                limit,
                sortDirection,
                sortBy
        );

        List<UserDto> fetched = userRepository.searchUsers(normalizedCondition, limit + 1)
                .stream()
                .map(UserMapper::toDto)
                .toList();

        long totalCount = userRepository.countUsers(normalizedCondition);

        return CursorResponse.of(
                fetched,
                limit,
                totalCount,
                sortBy,
                sortDirection,
                userDto -> extractCursor(userDto, sortBy),
                UserDto::id
        );
    }

    @Transactional
    public UserDto updateUserRole(UUID requesterId, UUID userId, UserRoleUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateRoleChangeAllowed(requesterId, user);

        user.changeRole(request.role());

        revokeUserSessions(userId);
        eventPublisher.publishEvent(new NotificationEvent(
                userId,
                "권한 변경",
                "사용자 권한이 %s(으)로 변경되었습니다.".formatted(request.role()),
                NotificationLevel.INFO
        ));

        return UserMapper.toDto(user);
    }

    @Transactional
    public UserDto updateUserLocked(UUID requesterId, UUID userId, UserLockUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateLockChangeAllowed(requesterId, user, request.locked());

        UserStatus status = request.locked()
                ? UserStatus.LOCKED
                : UserStatus.ACTIVE;

        user.changeStatus(status);

        if (status == UserStatus.LOCKED) {
            revokeUserSessions(userId);
        }

        return UserMapper.toDto(user);
    }

    private void validateRoleChangeAllowed(UUID requesterId, User targetUser) {
        if (targetUser.getId().equals(requesterId)) {
            throw new BusinessException(ErrorCode.SELF_ROLE_CHANGE_NOT_ALLOWED);
        }

        if (isSystemAdmin(targetUser)) {
            throw new BusinessException(ErrorCode.SYSTEM_ADMIN_ROLE_CHANGE_NOT_ALLOWED);
        }
    }

    private void validateLockChangeAllowed(UUID requesterId, User targetUser, boolean locked) {
        if (!locked) {
            return;
        }

        if (targetUser.getId().equals(requesterId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        if (isSystemAdmin(targetUser)) {
            throw new BusinessException(ErrorCode.SYSTEM_ADMIN_LOCK_NOT_ALLOWED);
        }
    }

    private boolean isSystemAdmin(User user) {
        return user.getEmail().equalsIgnoreCase(adminAccountProperties.email());
    }

    private void revokeUserSessions(UUID userId) {
        Instant now = Instant.now(clock);

        Iterable<AuthSession> sessions = authSessionRepository.findAllByUserId(userId);

        for (AuthSession session : sessions) {
            revokeSessionIfOwnedBy(session.getId(), userId, now);
        }
    }

    private void revokeSessionIfOwnedBy(UUID sessionId, UUID userId, Instant now) {
        authSessionLockManager.executeWithLock(sessionId, () -> {
            AuthSession currentSession = authSessionRepository.findById(sessionId)
                    .orElse(null);

            if (currentSession == null || !currentSession.getUserId().equals(userId)) {
                return;
            }

            currentSession.revoke(now);
            authSessionRepository.save(currentSession);
        });
    }

    private String normalizeSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return DEFAULT_SORT_BY;
        }

        if (!ALLOWED_SORT_BY.contains(sortBy)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return sortBy;
    }

    private String normalizeSortDirection(String sortDirection) {
        if (sortDirection == null || sortDirection.isBlank()) {
            return DEFAULT_SORT_DIRECTION;
        }

        String normalized = sortDirection.toUpperCase(Locale.ROOT);

        return switch (normalized) {
            case "ASC", "ASCENDING" -> "ASCENDING";
            case "DESC", "DESCENDING" -> "DESCENDING";
            default -> throw new BusinessException(ErrorCode.INVALID_SORT_DIRECTION);
        };
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return CursorPageRequest.DEFAULT_SIZE;
        }

        return Math.min(limit, CursorPageRequest.MAX_SIZE);
    }

    private String extractCursor(UserDto userDto, String sortBy) {
        return switch (sortBy) {
            case "name" -> userDto.name();
            case "email" -> userDto.email();
            case "createdAt" -> userDto.createdAt().toString();
            case "isLocked" -> String.valueOf(userDto.locked());
            case "role" -> userDto.role().name();
            default -> throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        };
    }
}
