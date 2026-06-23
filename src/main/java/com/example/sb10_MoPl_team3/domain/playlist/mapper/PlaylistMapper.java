package com.example.sb10_MoPl_team3.domain.playlist.mapper;

import com.example.sb10_MoPl_team3.domain.playlist.dto.PlaylistDto;
import com.example.sb10_MoPl_team3.domain.playlist.entity.Playlist;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PlaylistMapper {
    PlaylistDto toDto(Playlist playlist);
}
