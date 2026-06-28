package com.example.sb10_MoPl_team3.user.service;

import com.example.sb10_MoPl_team3.global.cursor.CursorPageRequest;
import com.example.sb10_MoPl_team3.global.cursor.CursorResponse;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.user.dto.request.UserSearchCondition;
import com.example.sb10_MoPl_team3.user.dto.response.UserDto;
import com.example.sb10_MoPl_team3.user.mapper.UserMapper;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

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

    public CursorResponse<UserDto> findUsers(UserSearchCondition condition) {
        String sortBy = normalizeSortBy(condition.sortBy());
        String sortDirection = normalizeSortDirection(condition.sortDirection());
        int limit = normalizeLimit(condition.limit());

        List<UserDto> fetched = userRepository.searchUsers(condition, limit + 1)
                .stream()
                .map(UserMapper::toDto)
                .toList();

        long totalCount = userRepository.countUsers(condition);

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