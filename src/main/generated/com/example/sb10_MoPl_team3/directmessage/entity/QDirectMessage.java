package com.example.sb10_MoPl_team3.directmessage.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QDirectMessage is a Querydsl query type for DirectMessage
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDirectMessage extends EntityPathBase<DirectMessage> {

    private static final long serialVersionUID = -140379684L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QDirectMessage directMessage = new QDirectMessage("directMessage");

    public final com.example.sb10_MoPl_team3.global.base.QBaseEntity _super = new com.example.sb10_MoPl_team3.global.base.QBaseEntity(this);

    public final StringPath content = createString("content");

    public final com.example.sb10_MoPl_team3.conversation.entity.QConversation conversation;

    //inherited
    public final DateTimePath<java.time.Instant> createdAt = _super.createdAt;

    //inherited
    public final ComparablePath<java.util.UUID> id = _super.id;

    public final BooleanPath read = createBoolean("read");

    public final DateTimePath<java.time.Instant> readAt = createDateTime("readAt", java.time.Instant.class);

    public final com.example.sb10_MoPl_team3.user.entity.QUser receiver;

    public final com.example.sb10_MoPl_team3.user.entity.QUser sender;

    //inherited
    public final DateTimePath<java.time.Instant> updatedAt = _super.updatedAt;

    public QDirectMessage(String variable) {
        this(DirectMessage.class, forVariable(variable), INITS);
    }

    public QDirectMessage(Path<? extends DirectMessage> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QDirectMessage(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QDirectMessage(PathMetadata metadata, PathInits inits) {
        this(DirectMessage.class, metadata, inits);
    }

    public QDirectMessage(Class<? extends DirectMessage> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.conversation = inits.isInitialized("conversation") ? new com.example.sb10_MoPl_team3.conversation.entity.QConversation(forProperty("conversation"), inits.get("conversation")) : null;
        this.receiver = inits.isInitialized("receiver") ? new com.example.sb10_MoPl_team3.user.entity.QUser(forProperty("receiver")) : null;
        this.sender = inits.isInitialized("sender") ? new com.example.sb10_MoPl_team3.user.entity.QUser(forProperty("sender")) : null;
    }

}

