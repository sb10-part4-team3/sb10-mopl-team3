package com.example.sb10_MoPl_team3.playlist.controller;

import com.example.sb10_MoPl_team3.global.openapi.ApiErrorResponses;
import com.example.sb10_MoPl_team3.playlist.dto.request.PlaylistCreateRequest;
import com.example.sb10_MoPl_team3.playlist.dto.request.PlaylistFindAllRequest;
import com.example.sb10_MoPl_team3.playlist.dto.request.PlaylistUpdateRequest;
import com.example.sb10_MoPl_team3.playlist.dto.response.PlaylistDto;
import com.example.sb10_MoPl_team3.playlist.service.PlaylistService;
import com.example.sb10_MoPl_team3.review.dto.response.CursorResponsePlaylistDto;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
@Tag(name = "플레이리스트 관리", description = "플레이리스트 및 구독 관리 API")
@SecurityRequirement(name = "BearerAuth")
public class PlaylistController {
    private final PlaylistService playlistService;

    // 플레이리스트 생성
    @PostMapping
    @Operation(summary = "플레이리스트 생성")
    @ApiErrorResponses.Common
    @ApiResponse(responseCode = "201", description = "생성 성공", content = @Content(schema = @Schema(implementation = PlaylistDto.class)))
    public ResponseEntity<PlaylistDto> createPlaylist(
            @Valid @RequestBody PlaylistCreateRequest request
    ) {
        PlaylistDto response = playlistService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 플레이리스트 수정
    @PatchMapping(value = "/{playlistId}")
    @Operation(summary = "플레이리스트 수정")
    @ApiErrorResponses.Forbidden
    public ResponseEntity<PlaylistDto> updatePlaylist(
            @PathVariable UUID playlistId,
            @Valid @RequestBody PlaylistUpdateRequest request
            ) {
        PlaylistDto response = playlistService.update(playlistId, request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 플레이리스트 단건 조회
    @GetMapping(value = "/{playlistId}")
    @Operation(summary = "플레이리스트 단건 조회")
    @ApiErrorResponses.Common
    public ResponseEntity<PlaylistDto> findPlaylistById(
            @PathVariable UUID playlistId
    ) {
        PlaylistDto response = playlistService.findById(playlistId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 플레이리스트 목록 조회
    @GetMapping
    @Operation(summary = "플레이리스트 목록 조회 (커서 페이지네이션)")
    @ApiErrorResponses.Common
    public ResponseEntity<CursorResponsePlaylistDto<PlaylistDto>> findPlaylists(
            @ModelAttribute PlaylistFindAllRequest request
    ) {
        return ResponseEntity.ok(playlistService.findAll(request));
    }

    // 플레이리스트 삭제
    @DeleteMapping(value = "/{playlistId}")
    @Operation(summary = "플레이리스트 삭제")
    @ApiErrorResponses.Forbidden
    public ResponseEntity<Void> deletePlaylist(
            @PathVariable UUID playlistId
    ) {
        playlistService.delete(playlistId);
        return ResponseEntity.ok().build();
    }

    // 플레이리스트 구독
    @PostMapping("/{playlistId}/subscription")
    @Operation(summary = "플레이리스트 구독")
    @ApiErrorResponses.Common
    @ApiResponse(responseCode = "204", description = "구독 성공")
    public ResponseEntity<Void> subscribePlaylist(
            @PathVariable UUID playlistId
    ) {
        playlistService.subscribe(playlistId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // 플레이리스트 구독 취소
    @DeleteMapping("/{playlistId}/subscription")
    @Operation(summary = "플레이리스트 구독 취소")
    @ApiErrorResponses.Common
    @ApiResponse(responseCode = "204", description = "구독 취소 성공")
    public ResponseEntity<Void> unsubscribePlaylist(
            @PathVariable UUID playlistId
    ) {
        playlistService.unsubscribe(playlistId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // 플레이리스트 콘텐츠 추가
    @PostMapping("/{playlistId}/contents/{contentId}")
    @Operation(summary = "플레이리스트에 콘텐츠 추가")
    @ApiErrorResponses.Forbidden
    @ApiResponse(responseCode = "204", description = "콘텐츠 추가 성공")
    public ResponseEntity<Void> addContentToPlaylist(
            @PathVariable UUID playlistId,
            @PathVariable UUID contentId
    ) {
        playlistService.addContent(playlistId, contentId);
        return ResponseEntity.noContent().build();
    }

    // 플레이리스트 콘텐츠 제거
    @DeleteMapping("/{playlistId}/contents/{contentId}")
    @Operation(summary = "플레이리스트에서 콘텐츠 삭제")
    @ApiErrorResponses.Forbidden
    @ApiResponse(responseCode = "204", description = "콘텐츠 삭제 성공")
    public ResponseEntity<Void> removeContentFromPlaylist(
            @PathVariable UUID playlistId,
            @PathVariable UUID contentId
    ) {
        playlistService.removeContent(playlistId, contentId);
        return ResponseEntity.noContent().build();
    }

}
