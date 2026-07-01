package com.example.sb10_MoPl_team3.content.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QContentTag is a Querydsl query type for ContentTag
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QContentTag extends EntityPathBase<ContentTag> {

    private static final long serialVersionUID = -1865394146L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QContentTag contentTag = new QContentTag("contentTag");

    public final QContent content;

    public final QContentTagId id;

    public final QTag tag;

    public QContentTag(String variable) {
        this(ContentTag.class, forVariable(variable), INITS);
    }

    public QContentTag(Path<? extends ContentTag> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QContentTag(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QContentTag(PathMetadata metadata, PathInits inits) {
        this(ContentTag.class, metadata, inits);
    }

    public QContentTag(Class<? extends ContentTag> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.content = inits.isInitialized("content") ? new QContent(forProperty("content")) : null;
        this.id = inits.isInitialized("id") ? new QContentTagId(forProperty("id")) : null;
        this.tag = inits.isInitialized("tag") ? new QTag(forProperty("tag")) : null;
    }

}

