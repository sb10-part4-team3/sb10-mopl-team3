package com.example.sb10_MoPl_team3.playlist.service;

import com.example.sb10_MoPl_team3.playlist.dto.request.PlaylistCreateRequest;
import com.example.sb10_MoPl_team3.playlist.dto.request.PlaylistUpdateRequest;
import com.example.sb10_MoPl_team3.playlist.dto.response.PlaylistDto;

import java.util.UUID;

public interface PlaylistService {
    // 플레이리스트 생성
    public PlaylistDto create(PlaylistCreateRequest request);

    // 플레이리스트 수정
    public PlaylistDto update(UUID playlistId, PlaylistUpdateRequest request);

    // 플레이리스트 단건 조회
    public PlaylistDto findById(UUID playlistId);

    // 플레이리스트 구독
    public void subscribe(UUID playlistId);

    // 플레이리스트 구독 취소
    public void unsubscribe(UUID playlistId);

    // 플레이리스트 논리 삭제
    public void delete(UUID playlistId);

    // 플레이리스트 물리 삭제
    public void hardDelete(UUID playlistId);
}
