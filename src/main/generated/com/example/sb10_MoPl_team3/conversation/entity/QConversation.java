package com.example.sb10_MoPl_team3.conversation.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QConversation is a Querydsl query type for Conversation
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QConversation extends EntityPathBase<Conversation> {

    private static final long serialVersionUID = 1825571356L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QConversation conversation = new QConversation("conversation");

    public final DateTimePath<java.time.Instant> createdAt = createDateTime("createdAt", java.time.Instant.class);

    public final ComparablePath<java.util.UUID> id = createComparable("id", java.util.UUID.class);

    public final com.example.sb10_MoPl_team3.user.entity.QUser user1;

    public final com.example.sb10_MoPl_team3.user.entity.QUser user2;

    public QConversation(String variable) {
        this(Conversation.class, forVariable(variable), INITS);
    }

    public QConversation(Path<? extends Conversation> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QConversation(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QConversation(PathMetadata metadata, PathInits inits) {
        this(Conversation.class, metadata, inits);
    }

    public QConversation(Class<? extends Conversation> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user1 = inits.isInitialized("user1") ? new com.example.sb10_MoPl_team3.user.entity.QUser(forProperty("user1")) : null;
        this.user2 = inits.isInitialized("user2") ? new com.example.sb10_MoPl_team3.user.entity.QUser(forProperty("user2")) : null;
    }

}

