package com.example.sb10_MoPl_team3.playlist.mapper;

import com.example.sb10_MoPl_team3.content.dto.ContentSummary;
import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.mapper.UserMapper;
import com.example.sb10_MoPl_team3.playlist.dto.response.PlaylistDto;
import com.example.sb10_MoPl_team3.playlist.entity.Playlist;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PlaylistMapper {
    @Mapping(source = "subscribedByMe", target = "subscribedByMe")
    @Mapping(source = "contents",target = "contents")
    @Mapping(source = "playlist.owner", target = "owner")
    PlaylistDto toDto(Playlist playlist, boolean subscribedByMe, List<ContentSummary> contents);

    default UserSummary mapOwner(User owner) {
        if (owner == null) {
            return null;
        }

        return UserMapper.toSummary(owner);
    }
}
