package com.example.sb10_MoPl_team3.playlist.controller;

import com.example.sb10_MoPl_team3.playlist.dto.request.PlaylistCreateRequest;
import com.example.sb10_MoPl_team3.playlist.dto.request.PlaylistUpdateRequest;
import com.example.sb10_MoPl_team3.playlist.dto.response.PlaylistDto;
import com.example.sb10_MoPl_team3.playlist.service.PlaylistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
public class PlaylistController {
    private final PlaylistService playlistService;

    // 플레이리스트 생성
    @PostMapping
    public ResponseEntity<PlaylistDto> createPlaylist(
            @Valid @RequestBody PlaylistCreateRequest request
    ) {
        PlaylistDto response = playlistService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 플레이리스트 수정
    @PatchMapping(value = "/{playlistId}")
    public ResponseEntity<PlaylistDto> updatePlaylist(
            @PathVariable UUID playlistId,
            @RequestBody PlaylistUpdateRequest request
            ) {
        PlaylistDto response = playlistService.update(playlistId, request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 플레이리스트 단건 조회
    @GetMapping(value = "/{playlistId}")
    public ResponseEntity<PlaylistDto> findPlaylistById(
            @PathVariable UUID playlistId
    ) {
        PlaylistDto response = playlistService.findById(playlistId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 플레이리스트 삭제
    @DeleteMapping(value = "/{playlistId}")
    public ResponseEntity<Void> deletePlaylist(
            @PathVariable UUID playlistId
    ) {
        playlistService.delete(playlistId);
        return ResponseEntity.ok().build();
    }
}
