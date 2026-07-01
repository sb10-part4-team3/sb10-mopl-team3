package com.example.sb10_MoPl_team3.content.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QContentTagId is a Querydsl query type for ContentTagId
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QContentTagId extends BeanPath<ContentTagId> {

    private static final long serialVersionUID = -1642409511L;

    public static final QContentTagId contentTagId = new QContentTagId("contentTagId");

    public final ComparablePath<java.util.UUID> contentId = createComparable("contentId", java.util.UUID.class);

    public final ComparablePath<java.util.UUID> tagId = createComparable("tagId", java.util.UUID.class);

    public QContentTagId(String variable) {
        super(ContentTagId.class, forVariable(variable));
    }

    public QContentTagId(Path<? extends ContentTagId> path) {
        super(path.getType(), path.getMetadata());
    }

    public QContentTagId(PathMetadata metadata) {
        super(ContentTagId.class, metadata);
    }

}

