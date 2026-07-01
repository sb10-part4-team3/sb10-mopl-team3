package com.example.sb10_MoPl_team3.playlist.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPlaylistSubscriber is a Querydsl query type for PlaylistSubscriber
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPlaylistSubscriber extends EntityPathBase<PlaylistSubscriber> {

    private static final long serialVersionUID = 835391426L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPlaylistSubscriber playlistSubscriber = new QPlaylistSubscriber("playlistSubscriber");

    public final com.example.sb10_MoPl_team3.global.base.QBaseEntity _super = new com.example.sb10_MoPl_team3.global.base.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.Instant> createdAt = _super.createdAt;

    //inherited
    public final ComparablePath<java.util.UUID> id = _super.id;

    public final QPlaylist playlist;

    //inherited
    public final DateTimePath<java.time.Instant> updatedAt = _super.updatedAt;

    public final com.example.sb10_MoPl_team3.user.entity.QUser user;

    public QPlaylistSubscriber(String variable) {
        this(PlaylistSubscriber.class, forVariable(variable), INITS);
    }

    public QPlaylistSubscriber(Path<? extends PlaylistSubscriber> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPlaylistSubscriber(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPlaylistSubscriber(PathMetadata metadata, PathInits inits) {
        this(PlaylistSubscriber.class, metadata, inits);
    }

    public QPlaylistSubscriber(Class<? extends PlaylistSubscriber> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.playlist = inits.isInitialized("playlist") ? new QPlaylist(forProperty("playlist"), inits.get("playlist")) : null;
        this.user = inits.isInitialized("user") ? new com.example.sb10_MoPl_team3.user.entity.QUser(forProperty("user")) : null;
    }

}

