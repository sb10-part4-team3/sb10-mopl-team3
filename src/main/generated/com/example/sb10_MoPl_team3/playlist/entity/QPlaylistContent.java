package com.example.sb10_MoPl_team3.playlist.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPlaylistContent is a Querydsl query type for PlaylistContent
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPlaylistContent extends EntityPathBase<PlaylistContent> {

    private static final long serialVersionUID = -1492369569L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPlaylistContent playlistContent = new QPlaylistContent("playlistContent");

    public final com.example.sb10_MoPl_team3.global.base.QBaseEntity _super = new com.example.sb10_MoPl_team3.global.base.QBaseEntity(this);

    public final com.example.sb10_MoPl_team3.content.entity.QContent content;

    //inherited
    public final DateTimePath<java.time.Instant> createdAt = _super.createdAt;

    //inherited
    public final ComparablePath<java.util.UUID> id = _super.id;

    public final QPlaylist playlist;

    //inherited
    public final DateTimePath<java.time.Instant> updatedAt = _super.updatedAt;

    public QPlaylistContent(String variable) {
        this(PlaylistContent.class, forVariable(variable), INITS);
    }

    public QPlaylistContent(Path<? extends PlaylistContent> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPlaylistContent(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPlaylistContent(PathMetadata metadata, PathInits inits) {
        this(PlaylistContent.class, metadata, inits);
    }

    public QPlaylistContent(Class<? extends PlaylistContent> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.content = inits.isInitialized("content") ? new com.example.sb10_MoPl_team3.content.entity.QContent(forProperty("content")) : null;
        this.playlist = inits.isInitialized("playlist") ? new QPlaylist(forProperty("playlist"), inits.get("playlist")) : null;
    }

}

