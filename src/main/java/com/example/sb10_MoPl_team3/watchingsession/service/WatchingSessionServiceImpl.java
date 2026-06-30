package com.example.sb10_MoPl_team3.watchingsession.service;

import com.example.sb10_MoPl_team3.content.dto.ContentSummary;
import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.entity.ContentStats;
import com.example.sb10_MoPl_team3.content.mapper.ContentMapper;
import com.example.sb10_MoPl_team3.content.repository.ContentStatsRepository;
import com.example.sb10_MoPl_team3.content.repository.ContentTagRepository;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.user.mapper.UserMapper;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import com.example.sb10_MoPl_team3.watchingsession.dto.WatchingSessionDto;
import com.example.sb10_MoPl_team3.watchingsession.entity.WatchingSession;
import com.example.sb10_MoPl_team3.watchingsession.mapper.WatchingSessionMapper;
import com.example.sb10_MoPl_team3.watchingsession.repository.WatchingSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WatchingSessionServiceImpl implements WatchingSessionService {

    private final WatchingSessionRepository watchingSessionRepository;
    private final UserRepository userRepository;
    private final ContentStatsRepository contentStatsRepository;
    private final ContentTagRepository contentTagRepository;

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
