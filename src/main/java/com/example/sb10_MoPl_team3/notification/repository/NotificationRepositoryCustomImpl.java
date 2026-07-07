package com.example.sb10_MoPl_team3.notification.repository;

import static com.example.sb10_MoPl_team3.notification.entity.QNotification.notification;

import com.example.sb10_MoPl_team3.notification.entity.Notification;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

@RequiredArgsConstructor
public class NotificationRepositoryCustomImpl implements NotificationRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Notification> findByReceiverIdAsc(
            UUID receiverId,
            Instant cursor,
            UUID idAfter,
            Pageable pageable
    ) {
        return queryFactory
                .selectFrom(notification)
                .join(notification.receiver).fetchJoin()
                .where(
                        notification.receiver.id.eq(receiverId),
                        cursorAfterCondition(cursor, idAfter)
                )
                .orderBy(notification.createdAt.asc(), notification.id.asc())
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public List<Notification> findByReceiverIdDesc(
            UUID receiverId,
            Instant cursor,
            UUID idAfter,
            Pageable pageable
    ) {
        return queryFactory
                .selectFrom(notification)
                .join(notification.receiver).fetchJoin()
                .where(
                        notification.receiver.id.eq(receiverId),
                        cursorBeforeCondition(cursor, idAfter)
                )
                .orderBy(notification.createdAt.desc(), notification.id.desc())
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public long countByReceiverId(UUID receiverId) {
        Long count = queryFactory
                .select(notification.count())
                .from(notification)
                .where(notification.receiver.id.eq(receiverId))
                .fetchOne();

        return count == null ? 0L : count;
    }

    private BooleanExpression cursorAfterCondition(Instant cursor, UUID idAfter) {
        if (cursor == null) {
            return null;
        }

        return notification.createdAt.gt(cursor)
                .or(notification.createdAt.eq(cursor).and(notification.id.gt(idAfter)));
    }

    private BooleanExpression cursorBeforeCondition(Instant cursor, UUID idAfter) {
        if (cursor == null) {
            return null;
        }

        return notification.createdAt.lt(cursor)
                .or(notification.createdAt.eq(cursor).and(notification.id.lt(idAfter)));
    }
}
