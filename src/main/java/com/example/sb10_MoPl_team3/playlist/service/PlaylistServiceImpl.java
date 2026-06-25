package com.example.sb10_MoPl_team3.playlist.service;

import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.global.security.SecurityUtils;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtClaims;
import com.example.sb10_MoPl_team3.playlist.dto.request.PlaylistCreateRequest;
import com.example.sb10_MoPl_team3.playlist.dto.request.PlaylistUpdateRequest;
import com.example.sb10_MoPl_team3.playlist.dto.response.PlaylistDto;
import com.example.sb10_MoPl_team3.playlist.entity.Playlist;
import com.example.sb10_MoPl_team3.playlist.enums.PlaylistStatus;
import com.example.sb10_MoPl_team3.playlist.exception.PlaylistNotFoundException;
import com.example.sb10_MoPl_team3.playlist.exception.PlaylistOwnerMismatchException;
import com.example.sb10_MoPl_team3.playlist.mapper.PlaylistMapper;
import com.example.sb10_MoPl_team3.playlist.repository.PlaylistRepository;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlaylistServiceImpl implements PlaylistService{
    private final PlaylistRepository playlistRepository;
    private final PlaylistMapper playlistMapper;
    private final UserRepository userRepository;

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
        PlaylistDto playlistDto = playlistMapper.toDto(savedPlaylist);

        return playlistDto;
    }

    // 플레이리스트 수정
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

        return playlistMapper.toDto(targetPlaylist);
    }

    // 플레이리스트 단건 조회
    @Override
    public PlaylistDto findById(UUID playlistId) {
        Playlist targetPlaylist = getPlaylistOrThrow(playlistId);
        validatePlaylistStatus(targetPlaylist);

        return playlistMapper.toDto(targetPlaylist);
    }

    // 플레이리스트 논리 삭제
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
