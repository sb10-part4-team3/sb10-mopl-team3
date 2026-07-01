package com.example.sb10_MoPl_team3.content.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QContent is a Querydsl query type for Content
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QContent extends EntityPathBase<Content> {

    private static final long serialVersionUID = -486059556L;

    public static final QContent content = new QContent("content");

    public final com.example.sb10_MoPl_team3.global.base.QBaseEntity _super = new com.example.sb10_MoPl_team3.global.base.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.Instant> createdAt = _super.createdAt;

    public final DateTimePath<java.time.Instant> deletedAt = createDateTime("deletedAt", java.time.Instant.class);

    public final StringPath description = createString("description");

    public final StringPath externalId = createString("externalId");

    //inherited
    public final ComparablePath<java.util.UUID> id = _super.id;

    public final StringPath source = createString("source");

    public final StringPath thumbnailUrl = createString("thumbnailUrl");

    public final StringPath title = createString("title");

    public final EnumPath<com.example.sb10_MoPl_team3.content.ContentType> type = createEnum("type", com.example.sb10_MoPl_team3.content.ContentType.class);

    //inherited
    public final DateTimePath<java.time.Instant> updatedAt = _super.updatedAt;

    public QContent(String variable) {
        super(Content.class, forVariable(variable));
    }

    public QContent(Path<? extends Content> path) {
        super(path.getType(), path.getMetadata());
    }

    public QContent(PathMetadata metadata) {
        super(Content.class, metadata);
    }

}

