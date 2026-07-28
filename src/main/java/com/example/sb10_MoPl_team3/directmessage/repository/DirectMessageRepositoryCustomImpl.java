package com.example.sb10_MoPl_team3.directmessage.repository;

import static com.example.sb10_MoPl_team3.directmessage.entity.QDirectMessage.directMessage;

import com.example.sb10_MoPl_team3.directmessage.entity.DirectMessage;
import com.example.sb10_MoPl_team3.directmessage.entity.QDirectMessage;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

@RequiredArgsConstructor
public class DirectMessageRepositoryCustomImpl implements DirectMessageRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<DirectMessage> findByConversationIdWithCursorAsc(
        UUID conversationId,
        Instant cursor,
        UUID idAfter,
        Pageable pageable
    ) {
        return queryFactory
            .selectFrom(directMessage)
            .join(directMessage.conversation).fetchJoin()
            .join(directMessage.sender).fetchJoin()
            .join(directMessage.receiver).fetchJoin()
            .where(
                directMessage.conversation.id.eq(conversationId),
                cursorAfterCondition(cursor, idAfter)
            )
            .orderBy(directMessage.createdAt.asc(), directMessage.id.asc())
            .limit(pageable.getPageSize())
            .fetch();
    }

    @Override
    public List<DirectMessage> findByConversationIdWithCursorDesc(
        UUID conversationId,
        Instant cursor,
        UUID idAfter,
        Pageable pageable
    ) {
        return queryFactory
            .selectFrom(directMessage)
            .join(directMessage.conversation).fetchJoin()
            .join(directMessage.sender).fetchJoin()
            .join(directMessage.receiver).fetchJoin()
            .where(
                directMessage.conversation.id.eq(conversationId),
                cursorBeforeCondition(cursor, idAfter)
            )
            .orderBy(directMessage.createdAt.desc(), directMessage.id.desc())
            .limit(pageable.getPageSize())
            .fetch();
    }

    @Override
    public List<DirectMessage> findLatestMessagesByConversationIds(List<UUID> conversationIds) {
        if (conversationIds == null || conversationIds.isEmpty()) {
            return List.of();
        }

        QDirectMessage newerMessage = new QDirectMessage("newerMessage");

        return queryFactory
            .selectFrom(directMessage)
            .join(directMessage.conversation).fetchJoin()
            .join(directMessage.sender).fetchJoin()
            .join(directMessage.receiver).fetchJoin()
            .where(
                directMessage.conversation.id.in(conversationIds),
                JPAExpressions
                    .selectOne()
                    .from(newerMessage)
                    .where(
                        newerMessage.conversation.id.eq(directMessage.conversation.id),
                        newerMessage.createdAt.gt(directMessage.createdAt)
                            .or(newerMessage.createdAt.eq(directMessage.createdAt)
                                .and(newerMessage.id.gt(directMessage.id)))
                    )
                    .notExists()
            )
            .fetch();
    }

    @Override
    public Set<UUID> findUnreadConversationIds(List<UUID> conversationIds, UUID receiverId) {
        if (conversationIds == null || conversationIds.isEmpty()) {
            return Set.of();
        }

        return new HashSet<>(queryFactory
            .select(directMessage.conversation.id)
            .distinct()
            .from(directMessage)
            .where(
                directMessage.conversation.id.in(conversationIds),
                directMessage.receiver.id.eq(receiverId),
                directMessage.read.isFalse()
            )
            .fetch());
    }

    private BooleanExpression cursorAfterCondition(Instant cursor, UUID idAfter) {
        if (cursor == null) {
            return null;
        }

        return directMessage.createdAt.gt(cursor)
            .or(directMessage.createdAt.eq(cursor).and(directMessage.id.gt(idAfter)));
    }

    private BooleanExpression cursorBeforeCondition(Instant cursor, UUID idAfter) {
        if (cursor == null) {
            return null;
        }

        return directMessage.createdAt.lt(cursor)
            .or(directMessage.createdAt.eq(cursor).and(directMessage.id.lt(idAfter)));
    }
}
