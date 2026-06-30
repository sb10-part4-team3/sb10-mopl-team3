package com.example.sb10_MoPl_team3.watchingsession.repository;

import com.example.sb10_MoPl_team3.watchingsession.entity.WatchingSession;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.example.sb10_MoPl_team3.watchingsession.entity.QWatchingSession.watchingSession;

@RequiredArgsConstructor
public class WatchingSessionRepositoryCustomImpl implements WatchingSessionRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<WatchingSession> findByContentAsc(
            UUID contentId, String watcherName, Instant cursor, UUID idAfter, Pageable pageable
    ) {
        return queryFactory.selectFrom(watchingSession)
                .join(watchingSession.watcher).fetchJoin()
                .join(watchingSession.content).fetchJoin()
                .where(
                        watchingSession.content.id.eq(contentId),
                        watcherNameCondition(watcherName),
                        cursorAfterCondition(cursor, idAfter)
                )
                .orderBy(watchingSession.createdAt.asc(), watchingSession.id.asc())
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public List<WatchingSession> findByContentDesc(
            UUID contentId, String watcherName, Instant cursor, UUID idAfter, Pageable pageable
    ) {
        return queryFactory.selectFrom(watchingSession)
                .join(watchingSession.watcher).fetchJoin()
                .join(watchingSession.content).fetchJoin()
                .where(
                        watchingSession.content.id.eq(contentId),
                        watcherNameCondition(watcherName),
                        cursorBeforeCondition(cursor, idAfter)
                )
                .orderBy(watchingSession.createdAt.desc(), watchingSession.id.desc())
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public long countByContent(UUID contentId, String watcherName) {
        Long count = queryFactory.select(watchingSession.count())
                .from(watchingSession)
                .where(
                        watchingSession.content.id.eq(contentId),
                        watcherNameCondition(watcherName)
                )
                .fetchOne();
        return count == null ? 0L : count;
    }

    private BooleanExpression watcherNameCondition(String watcherName) {
        return watcherName == null || watcherName.isBlank()
                ? null
                : watchingSession.watcher.name.containsIgnoreCase(watcherName);
    }

    private BooleanExpression cursorAfterCondition(Instant cursor, UUID idAfter) {
        if (cursor == null) {
            return null;
        }
        return watchingSession.createdAt.gt(cursor)
                .or(watchingSession.createdAt.eq(cursor).and(watchingSession.id.gt(idAfter)));
    }

    private BooleanExpression cursorBeforeCondition(Instant cursor, UUID idAfter) {
        if (cursor == null) {
            return null;
        }
        return watchingSession.createdAt.lt(cursor)
                .or(watchingSession.createdAt.eq(cursor).and(watchingSession.id.lt(idAfter)));
    }
}
