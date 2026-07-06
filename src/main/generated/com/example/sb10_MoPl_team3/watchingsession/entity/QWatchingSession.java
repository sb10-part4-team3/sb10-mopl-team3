package com.example.sb10_MoPl_team3.watchingsession.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QWatchingSession is a Querydsl query type for WatchingSession
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QWatchingSession extends EntityPathBase<WatchingSession> {

    private static final long serialVersionUID = -1691442564L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QWatchingSession watchingSession = new QWatchingSession("watchingSession");

    public final com.example.sb10_MoPl_team3.content.entity.QContent content;

    public final DateTimePath<java.time.Instant> createdAt = createDateTime("createdAt", java.time.Instant.class);

    public final ComparablePath<java.util.UUID> id = createComparable("id", java.util.UUID.class);

    public final com.example.sb10_MoPl_team3.user.entity.QUser watcher;

    public QWatchingSession(String variable) {
        this(WatchingSession.class, forVariable(variable), INITS);
    }

    public QWatchingSession(Path<? extends WatchingSession> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QWatchingSession(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QWatchingSession(PathMetadata metadata, PathInits inits) {
        this(WatchingSession.class, metadata, inits);
    }

    public QWatchingSession(Class<? extends WatchingSession> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.content = inits.isInitialized("content") ? new com.example.sb10_MoPl_team3.content.entity.QContent(forProperty("content")) : null;
        this.watcher = inits.isInitialized("watcher") ? new com.example.sb10_MoPl_team3.user.entity.QUser(forProperty("watcher")) : null;
    }

}

