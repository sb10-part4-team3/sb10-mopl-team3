package com.example.sb10_MoPl_team3.watchingsession.service;

import com.example.sb10_MoPl_team3.content.dto.ContentSummary;
import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.entity.ContentStats;
import com.example.sb10_MoPl_team3.content.mapper.ContentMapper;
import com.example.sb10_MoPl_team3.content.repository.ContentStatsRepository;
import com.example.sb10_MoPl_team3.content.repository.ContentTagRepository;
import com.example.sb10_MoPl_team3.content.repository.ContentRepository;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.user.mapper.UserMapper;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import com.example.sb10_MoPl_team3.watchingsession.dto.CursorResponseWatchingSessionDto;
import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionDto;
import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionFindAllRequest;
import com.example.sb10_MoPl_team3.watchingsession.entity.WatchingSession;
import com.example.sb10_MoPl_team3.watchingsession.mapper.WatchingSessionMapper;
import com.example.sb10_MoPl_team3.watchingsession.repository.WatchingSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WatchingSessionServiceImpl implements WatchingSessionService {

    private final WatchingSessionRepository watchingSessionRepository;
    private final UserRepository userRepository;
    private final ContentStatsRepository contentStatsRepository;
    private final ContentTagRepository contentTagRepository;
    private final ContentRepository contentRepository;

    @Override
    @Nullable
    public WatchingSessionDto findByWatcher(UUID watcherId) {
        if (!userRepository.existsById(watcherId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return watchingSessionRepository.findByWatcherId(watcherId)
                .map(this::toDto)
                .orElse(null);
    }

    @Override
    public CursorResponseWatchingSessionDto findByContent(WatchingSessionFindAllRequest request) {
        if (!contentRepository.existsById(request.contentId())) {
            throw new BusinessException(ErrorCode.CONTENT_NOT_FOUND);
        }

        String sortBy = request.sortBy() == null || request.sortBy().isBlank()
                ? "createdAt" : request.sortBy();
        if (!"createdAt".equals(sortBy)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        String sortDirection = request.sortDirection() == null
                ? "DESCENDING" : request.sortDirection().toUpperCase(Locale.ROOT);
        if (!sortDirection.equals("ASCENDING") && !sortDirection.equals("DESCENDING")) {
            throw new BusinessException(ErrorCode.INVALID_SORT_DIRECTION);
        }
        boolean ascending = sortDirection.equals("ASCENDING");
        int limit = request.limit() <= 0 ? 20 : Math.min(request.limit(), 100);

        boolean hasCursor = request.cursor() != null && !request.cursor().isBlank();
        boolean hasIdAfter = request.idAfter() != null;
        if ((hasCursor && !hasIdAfter) || (!hasCursor && hasIdAfter)) {
            throw new BusinessException(ErrorCode.INVALID_CURSOR);
        }
        Instant cursor = null;
        if (hasCursor) {
            try {
                cursor = Instant.parse(request.cursor());
            } catch (RuntimeException exception) {
                throw new BusinessException(ErrorCode.INVALID_CURSOR);
            }
        }

        String watcherName = request.watcherNameLike() == null ? "" : request.watcherNameLike().trim();
        PageRequest pageRequest = PageRequest.of(0, limit + 1);
        List<WatchingSession> fetched = ascending
                ? watchingSessionRepository.findByContentAsc(
                        request.contentId(), watcherName, cursor, request.idAfter(), pageRequest)
                : watchingSessionRepository.findByContentDesc(
                        request.contentId(), watcherName, cursor, request.idAfter(), pageRequest);
        boolean hasNext = fetched.size() > limit;
        List<WatchingSession> sessions = hasNext ? fetched.subList(0, limit) : fetched;

        String nextCursor = null;
        UUID nextIdAfter = null;
        if (hasNext && !sessions.isEmpty()) {
            WatchingSession last = sessions.get(sessions.size() - 1);
            nextCursor = last.getCreatedAt().toString();
            nextIdAfter = last.getId();
        }

        List<WatchingSessionDto> data = sessions.stream().map(this::toDto).toList();
        long totalCount = watchingSessionRepository.countByContent(request.contentId(), watcherName);
        return new CursorResponseWatchingSessionDto(
                data, nextCursor, nextIdAfter, hasNext, totalCount, sortBy, sortDirection
        );
    }

    private WatchingSessionDto toDto(WatchingSession watchingSession) {
        Content content = watchingSession.getContent();
        ContentStats stats = contentStatsRepository.findById(content.getId()).orElse(null);
        List<String> tags = contentTagRepository.findTagNamesByContentId(content.getId());
        ContentSummary contentSummary = ContentMapper.toSummary(content, stats, tags);

        return WatchingSessionMapper.toDto(
                watchingSession,
                UserMapper.toSummary(watchingSession.getWatcher()),
                contentSummary
        );
    }
}
