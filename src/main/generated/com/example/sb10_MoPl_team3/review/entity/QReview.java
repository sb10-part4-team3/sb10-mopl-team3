package com.example.sb10_MoPl_team3.review.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QReview is a Querydsl query type for Review
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReview extends EntityPathBase<Review> {

    private static final long serialVersionUID = -1744128890L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QReview review = new QReview("review");

    public final com.example.sb10_MoPl_team3.global.base.QBaseEntity _super = new com.example.sb10_MoPl_team3.global.base.QBaseEntity(this);

    public final com.example.sb10_MoPl_team3.user.entity.QUser author;

    public final com.example.sb10_MoPl_team3.content.entity.QContent content;

    //inherited
    public final DateTimePath<java.time.Instant> createdAt = _super.createdAt;

    public final DateTimePath<java.time.Instant> deletedAt = createDateTime("deletedAt", java.time.Instant.class);

    //inherited
    public final ComparablePath<java.util.UUID> id = _super.id;

    public final NumberPath<Double> rating = createNumber("rating", Double.class);

    public final EnumPath<com.example.sb10_MoPl_team3.review.enums.ReviewStatus> status = createEnum("status", com.example.sb10_MoPl_team3.review.enums.ReviewStatus.class);

    public final StringPath text = createString("text");

    //inherited
    public final DateTimePath<java.time.Instant> updatedAt = _super.updatedAt;

    public QReview(String variable) {
        this(Review.class, forVariable(variable), INITS);
    }

    public QReview(Path<? extends Review> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QReview(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QReview(PathMetadata metadata, PathInits inits) {
        this(Review.class, metadata, inits);
    }

    public QReview(Class<? extends Review> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.author = inits.isInitialized("author") ? new com.example.sb10_MoPl_team3.user.entity.QUser(forProperty("author")) : null;
        this.content = inits.isInitialized("content") ? new com.example.sb10_MoPl_team3.content.entity.QContent(forProperty("content")) : null;
    }

}

