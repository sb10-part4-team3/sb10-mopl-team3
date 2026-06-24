package com.example.sb10_MoPl_team3.playlist.service;

import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtClaims;
import com.example.sb10_MoPl_team3.playlist.dto.request.PlaylistCreateRequest;
import com.example.sb10_MoPl_team3.playlist.dto.response.PlaylistDto;
import com.example.sb10_MoPl_team3.playlist.entity.Playlist;
import com.example.sb10_MoPl_team3.playlist.enums.PlaylistStatus;
import com.example.sb10_MoPl_team3.playlist.exception.PlaylistNotFoundException;
import com.example.sb10_MoPl_team3.playlist.mapper.PlaylistMapper;
import com.example.sb10_MoPl_team3.playlist.repository.PlaylistRepository;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlaylistServiceImpl implements PlaylistService{
    private final PlaylistRepository playlistRepository;
    private final PlaylistMapper playlistMapper;
    private final UserRepository userRepository;

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

    @Override
    public void delete(UUID playlistId) {
        UUID requestUserId = getAuthenticatedUserId();
        Playlist targetPlaylist = getPlaylistOrThrow(playlistId);


    }

    @Override
    public void hardDelete(UUID playlistId) {
        UUID requestUserId = getAuthenticatedUserId();
    }


    // 인증 사용자 조회
    private UUID getAuthenticatedUserId() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof JwtClaims jwtClaims)) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIAL);
        }

        return jwtClaims.userId();
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
}
