package com.example.sb10_MoPl_team3.playlist.repository;

import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.playlist.entity.Playlist;
import com.example.sb10_MoPl_team3.playlist.entity.PlaylistContent;
import com.example.sb10_MoPl_team3.playlist.entity.QPlaylist;
import com.example.sb10_MoPl_team3.playlist.entity.QPlaylistContent;
import com.example.sb10_MoPl_team3.playlist.enums.PlaylistStatus;
import com.example.sb10_MoPl_team3.playlist.exception.PlaylistNotFoundException;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PlaylistContentRepositoryImpl implements PlaylistContentRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    @Override
    public int insertIfNotExists(Playlist playlist, Content content) {
        QPlaylist playlistPath = QPlaylist.playlist;
        QPlaylistContent playlistContent = QPlaylistContent.playlistContent;

        Playlist lockedPlaylist = queryFactory
                .selectFrom(playlistPath)
                .where(playlistPath.id.eq(playlist.getId()))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetchOne();

        if (lockedPlaylist == null || lockedPlaylist.getStatus() == PlaylistStatus.DELETED) {
            throw new PlaylistNotFoundException(playlist.getId());
        }

        boolean exists = queryFactory
                .selectOne()
                .from(playlistContent)
                .where(
                        playlistContent.playlist.id.eq(playlist.getId()),
                        playlistContent.content.id.eq(content.getId())
                )
                .fetchFirst() != null;

        if (exists) {
            return 0;
        }

        entityManager.persist(new PlaylistContent(lockedPlaylist, content));
        return 1;
    }
}
