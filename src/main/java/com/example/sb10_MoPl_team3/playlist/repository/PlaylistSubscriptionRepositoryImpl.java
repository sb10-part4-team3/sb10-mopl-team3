package com.example.sb10_MoPl_team3.playlist.repository;

import com.example.sb10_MoPl_team3.playlist.entity.Playlist;
import com.example.sb10_MoPl_team3.playlist.entity.PlaylistSubscriber;
import com.example.sb10_MoPl_team3.playlist.entity.QPlaylist;
import com.example.sb10_MoPl_team3.playlist.entity.QPlaylistSubscriber;
import com.example.sb10_MoPl_team3.playlist.enums.PlaylistStatus;
import com.example.sb10_MoPl_team3.playlist.exception.PlaylistNotFoundException;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PlaylistSubscriptionRepositoryImpl implements PlaylistSubscriptionRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    @Override
    public int insertIfNotExists(Playlist playlist, User user) {
        QPlaylist playlistPath = QPlaylist.playlist;
        QPlaylistSubscriber subscriber = QPlaylistSubscriber.playlistSubscriber;

        Playlist lockedPlaylist = queryFactory
                .selectFrom(playlistPath)
                .where(playlistPath.id.eq(playlist.getId()))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetchOne();

        if (lockedPlaylist == null || lockedPlaylist.getStatus() == PlaylistStatus.DELETED) {
            throw new PlaylistNotFoundException(playlist.getId());
        }

        Boolean exists = queryFactory
                .selectOne()
                .from(subscriber)
                .where(
                        subscriber.playlist.id.eq(playlist.getId()),
                        subscriber.user.id.eq(user.getId())
                )
                .fetchFirst() != null;

        if (Boolean.TRUE.equals(exists)) {
            return 0;
        }

        entityManager.persist(
                PlaylistSubscriber.builder()
                        .playlist(lockedPlaylist)
                        .user(user)
                        .build()
        );

        return 1;
    }
}
