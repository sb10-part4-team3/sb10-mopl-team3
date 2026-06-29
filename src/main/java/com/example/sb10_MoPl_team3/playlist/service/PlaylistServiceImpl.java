package com.example.sb10_MoPl_team3.playlist.service;

import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.repository.ContentRepository;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.global.security.SecurityUtils;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtClaims;
import com.example.sb10_MoPl_team3.playlist.dto.request.PlaylistCreateRequest;
import com.example.sb10_MoPl_team3.playlist.dto.request.PlaylistUpdateRequest;
import com.example.sb10_MoPl_team3.playlist.dto.response.PlaylistDto;
import com.example.sb10_MoPl_team3.playlist.entity.Playlist;
import com.example.sb10_MoPl_team3.playlist.entity.PlaylistContent;
import com.example.sb10_MoPl_team3.playlist.entity.PlaylistSubscriber;
import com.example.sb10_MoPl_team3.playlist.enums.PlaylistStatus;
import com.example.sb10_MoPl_team3.playlist.exception.PlaylistNotFoundException;
import com.example.sb10_MoPl_team3.playlist.exception.PlaylistOwnerMismatchException;
import com.example.sb10_MoPl_team3.playlist.mapper.PlaylistMapper;
import com.example.sb10_MoPl_team3.playlist.repository.PlaylistContentRepository;
import com.example.sb10_MoPl_team3.playlist.repository.PlaylistRepository;
import com.example.sb10_MoPl_team3.playlist.repository.PlaylistSubscriptionRepository;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlaylistServiceImpl implements PlaylistService{
    private final PlaylistRepository playlistRepository;
    private final PlaylistSubscriptionRepository playlistSubscriptionRepository;
    private final PlaylistMapper playlistMapper;
    private final UserRepository userRepository;
    private final PlaylistContentRepository playlistContentRepository;
    private final ContentRepository contentRepository;

    // 플레이리스트 생성
    @Override
    public PlaylistDto create(PlaylistCreateRequest request) {
        UUID requestUserId = getAuthenticatedUserId();

        User owner = getUserOrThrow(requestUserId);
        Playlist newPlaylist = Playlist.builder()
                .owner(owner)
                .title(request.title())
                .description(request.description())
                .status(PlaylistStatus.ACTIVE)
                .build();

        Playlist savedPlaylist = playlistRepository.save(newPlaylist);
        PlaylistDto playlistDto = playlistMapper.toDto(savedPlaylist, false);

        return playlistDto;
    }

    // 플레이리스트 수정
    @Transactional
    @Override
    public PlaylistDto update(UUID playlistId, PlaylistUpdateRequest request) {
        // 요청 조회
        UUID requestUserId = getAuthenticatedUserId();
        Playlist targetPlaylist = getPlaylistOrThrow(playlistId);

        // 유효성 검증
        validatePlaylistStatus(targetPlaylist);
        validatePlaylistOwner(targetPlaylist, requestUserId);

        // 수정
        targetPlaylist.update(request.title(), request.description());

        boolean isSubscribed = playlistSubscriptionRepository.existsByPlaylistIdAndUserId(targetPlaylist.getId(), requestUserId);
        return playlistMapper.toDto(targetPlaylist, isSubscribed);
    }

    // 플레이리스트 단건 조회
    @Transactional(readOnly = true)
    @Override
    public PlaylistDto findById(UUID playlistId) {
        UUID requestUserId = getAuthenticatedUserId();
        Playlist targetPlaylist = getPlaylistOrThrow(playlistId);

        validatePlaylistStatus(targetPlaylist);

        boolean isSubscribed = playlistSubscriptionRepository.existsByPlaylistIdAndUserId(playlistId, requestUserId);
        PlaylistDto playlistDto = playlistMapper.toDto(targetPlaylist, isSubscribed);

        return playlistDto;
    }

    // 플레이리스트 구독
    @Transactional
    @Override
    public void subscribe(UUID playlistId) {
        UUID requestUserId = getAuthenticatedUserId();

        Playlist playlist = getPlaylistOrThrow(playlistId);
        validatePlaylistStatus(playlist);

        if (playlist.getOwner().getId().equals(requestUserId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        User user = getUserOrThrow(requestUserId);

        try {
            playlistSubscriptionRepository.saveAndFlush(
                    new PlaylistSubscriber(playlist, user)
            );
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 사용자가 같은 플레이리스트를 구독했거나 이미 구독 중인 경우.
            // 유니크 제약이 보장하므로 카운트는 증가시키지 않고 멱등 성공으로 처리한다.
            if (playlistSubscriptionRepository.existsByPlaylistIdAndUserId(playlistId, requestUserId)) {
                return;
            }
            throw e;
        }

        int updated = playlistRepository.increaseSubscriberCount(
                playlistId,
                PlaylistStatus.DELETED
        );

        if (updated == 0) {
            throw new PlaylistNotFoundException(playlistId);
        }
    }

    // 플레이리스트 구독 취소
    @Transactional
    @Override
    public void unsubscribe(UUID playlistId) {
        UUID requestUserId = getAuthenticatedUserId();

        Playlist playlist = getPlaylistOrThrow(playlistId);
        validatePlaylistStatus(playlist);

        int deleted = playlistSubscriptionRepository.deleteByPlaylistIdAndUserId(
                playlistId,
                requestUserId
        );

        if (deleted == 0) {
            // 이미 구독 취소된 상태면 멱등 성공.
            return;
        }

        int updated = playlistRepository.decreaseSubscriberCount(
                playlistId,
                PlaylistStatus.DELETED
        );

        if (updated == 0) {
            throw new PlaylistNotFoundException(playlistId);
        }
    }

    // 플레이리스트 콘텐츠 추가
    @Transactional
    @Override
    public void addContent(UUID playlistId, UUID contentId) {
        UUID requestUserId = getAuthenticatedUserId();

        Playlist playlist = getPlaylistOrThrow(playlistId);
        validatePlaylistStatus(playlist);
        validatePlaylistOwner(playlist, requestUserId);

        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));

        if (playlistContentRepository.existsByPlaylistIdAndContentId(playlistId, contentId)) {
            return;
        }

        playlistContentRepository.save(new PlaylistContent(playlist, content));
    }

    // 플레이리스트 콘텐츠 제거
    @Transactional
    @Override
    public void removeContent(UUID playlistId, UUID contentId) {
        UUID requestUserId = getAuthenticatedUserId();

        Playlist playlist = getPlaylistOrThrow(playlistId);
        validatePlaylistStatus(playlist);
        validatePlaylistOwner(playlist, requestUserId);

        if (!contentRepository.existsById(contentId)) {
            throw new BusinessException(ErrorCode.CONTENT_NOT_FOUND);
        }

        playlistContentRepository.deleteByPlaylistIdAndContentId(playlistId, contentId);
    }

    // 플레이리스트 논리 삭제
    @Transactional
    @Override
    public void delete(UUID playlistId) {
        UUID requestUserId = getAuthenticatedUserId();
        Playlist targetPlaylist = getPlaylistOrThrow(playlistId);

        validatePlaylistStatus(targetPlaylist);
        validatePlaylistOwner(targetPlaylist, requestUserId);

        targetPlaylist.delete();
    }

    // 플레이리스트 물리 삭제
    @Override
    public void hardDelete(UUID playlistId) {
        UUID requestUserId = getAuthenticatedUserId();
        Playlist targetPlaylist = getPlaylistOrThrow(playlistId);

        validatePlaylistStatus(targetPlaylist);
        validatePlaylistOwner(targetPlaylist, requestUserId);

        playlistRepository.delete(targetPlaylist);
    }


    // 인증 사용자 조회
    private UUID getAuthenticatedUserId() {
        return SecurityUtils.getCurrentUserId();
    }

    // 유저 조회 후 반환
    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    // 플레이리스트 조회 후 반환
    private Playlist getPlaylistOrThrow(UUID playlistId) {
        return playlistRepository.findById(playlistId)
                .orElseThrow(() -> new PlaylistNotFoundException(playlistId));
    }

    // 유효성 검증(플레이리스트 상태 검증)
    private void validatePlaylistStatus(Playlist playlist) {
        if (playlist.getStatus() == PlaylistStatus.DELETED) {
            throw new PlaylistNotFoundException(playlist.getId());
        }
    }

    // 유효성 검증(플레이리스트 소유자 검증)
    private void validatePlaylistOwner(Playlist playlist, UUID userId) {
        if (!playlist.getOwner().getId().equals(userId)) {
            throw new PlaylistOwnerMismatchException(userId, playlist.getId());
        }
    }
}
