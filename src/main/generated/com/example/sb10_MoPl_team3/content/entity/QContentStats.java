package com.example.sb10_MoPl_team3.content.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QContentStats is a Querydsl query type for ContentStats
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QContentStats extends EntityPathBase<ContentStats> {

    private static final long serialVersionUID = -1642771421L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QContentStats contentStats = new QContentStats("contentStats");

    public final NumberPath<java.math.BigDecimal> averageRating = createNumber("averageRating", java.math.BigDecimal.class);

    public final QContent content;

    public final ComparablePath<java.util.UUID> id = createComparable("id", java.util.UUID.class);

    public final NumberPath<Integer> reviewCount = createNumber("reviewCount", Integer.class);

    public final DateTimePath<java.time.Instant> updatedAt = createDateTime("updatedAt", java.time.Instant.class);

    public final NumberPath<Integer> viewerCount = createNumber("viewerCount", Integer.class);

    public QContentStats(String variable) {
        this(ContentStats.class, forVariable(variable), INITS);
    }

    public QContentStats(Path<? extends ContentStats> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QContentStats(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QContentStats(PathMetadata metadata, PathInits inits) {
        this(ContentStats.class, metadata, inits);
    }

    public QContentStats(Class<? extends ContentStats> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.content = inits.isInitialized("content") ? new QContent(forProperty("content")) : null;
    }

}

