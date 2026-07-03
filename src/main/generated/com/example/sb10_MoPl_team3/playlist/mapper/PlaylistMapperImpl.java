package com.example.sb10_MoPl_team3.playlist.mapper;

import com.example.sb10_MoPl_team3.content.dto.ContentSummary;
import com.example.sb10_MoPl_team3.playlist.dto.response.PlaylistDto;
import com.example.sb10_MoPl_team3.playlist.entity.Playlist;
import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-01T11:26:32+0900",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.17 (Azul Systems, Inc.)"
)
@Component
public class PlaylistMapperImpl implements PlaylistMapper {

    @Override
    public PlaylistDto toDto(Playlist playlist, boolean subscribedByMe) {
        if ( playlist == null ) {
            return null;
        }

        UserSummary owner = null;
        UUID id = null;
        String title = null;
        String description = null;
        Instant updatedAt = null;
        Long subscriberCount = null;
        if ( playlist != null ) {
            owner = mapOwner( playlist.getOwner() );
            id = playlist.getId();
            title = playlist.getTitle();
            description = playlist.getDescription();
            updatedAt = playlist.getUpdatedAt();
            if ( playlist.getSubscriberCount() != null ) {
                subscriberCount = playlist.getSubscriberCount().longValue();
            }
        }
        boolean subscribedByMe1 = false;
        subscribedByMe1 = subscribedByMe;

        List<ContentSummary> contents = null;

        PlaylistDto playlistDto = new PlaylistDto( id, owner, title, description, updatedAt, subscriberCount, subscribedByMe1, contents );

        return playlistDto;
    }
}
