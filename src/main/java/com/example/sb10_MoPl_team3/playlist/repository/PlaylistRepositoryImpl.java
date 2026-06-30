// PlaylistRepositoryImpl.java
package com.example.sb10_MoPl_team3.playlist.repository;

import static com.example.sb10_MoPl_team3.playlist.entity.QPlaylist.playlist;
import static com.example.sb10_MoPl_team3.playlist.entity.QPlaylistSubscriber.playlistSubscriber;

import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.playlist.dto.request.PlaylistFindAllRequest;
import com.example.sb10_MoPl_team3.playlist.entity.Playlist;
import com.example.sb10_MoPl_team3.playlist.enums.PlaylistStatus;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PlaylistRepositoryImpl implements PlaylistRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public PlaylistSearchResult search(PlaylistFindAllRequest request) {
        String sortBy = normalizeSortBy(request.sortBy());
        String sortDirection = normalizeSortDirection(request.sortDirection());
        boolean ascending = sortDirection.equals("ASCENDING");
        int limit = request.limit() <= 0 ? 20 : Math.min(request.limit(), 100);

        BooleanBuilder where = baseWhere(request);
        applyCursor(where, request, sortBy, ascending);

        Order order = ascending ? Order.ASC : Order.DESC;
        OrderSpecifier<?> primaryOrder = sortBy.equals("updatedAt")
                ? new OrderSpecifier<>(order, playlist.updatedAt)
                : new OrderSpecifier<>(order, playlist.subscriberCount);

        List<Playlist> playlists = queryFactory
                .selectFrom(playlist)
                .leftJoin(playlist.owner).fetchJoin()
                .where(where)
                .orderBy(primaryOrder, new OrderSpecifier<>(order, playlist.id))
                .limit(limit + 1L)
                .fetch();

        Long totalCount = queryFactory
                .select(playlist.count())
                .from(playlist)
                .where(baseWhere(request))
                .fetchOne();

        return new PlaylistSearchResult(playlists, totalCount == null ? 0L : totalCount);
    }

    private BooleanBuilder baseWhere(PlaylistFindAllRequest request) {
        BooleanBuilder where = new BooleanBuilder();
        where.and(playlist.status.eq(PlaylistStatus.ACTIVE));

        if (request.keywordLike() != null && !request.keywordLike().isBlank()) {
            where.and(
                    playlist.title.containsIgnoreCase(request.keywordLike())
                            .or(playlist.description.containsIgnoreCase(request.keywordLike()))
            );
        }

        if (request.ownerIdEqual() != null) {
            where.and(playlist.owner.id.eq(request.ownerIdEqual()));
        }

        if (request.subscriberIdEqual() != null) {
            where.and(
                    JPAExpressions.selectOne()
                            .from(playlistSubscriber)
                            .where(
                                    playlistSubscriber.playlist.id.eq(playlist.id),
                                    playlistSubscriber.user.id.eq(request.subscriberIdEqual())
                            )
                            .exists()
            );
        }

        return where;
    }

    private void applyCursor(
            BooleanBuilder where,
            PlaylistFindAllRequest request,
            String sortBy,
            boolean ascending
    ) {
        boolean hasCursor = request.cursor() != null && !request.cursor().isBlank();
        boolean hasIdAfter = request.idAfter() != null;

        if (hasCursor != hasIdAfter) {
            throw new BusinessException(ErrorCode.INVALID_CURSOR);
        }

        if (!hasCursor) {
            return;
        }

        if (sortBy.equals("updatedAt")) {
            Instant cursor = parseInstantCursor(request.cursor());
            where.and(ascending
                    ? playlist.updatedAt.gt(cursor)
                    .or(playlist.updatedAt.eq(cursor).and(playlist.id.gt(request.idAfter())))
                    : playlist.updatedAt.lt(cursor)
                    .or(playlist.updatedAt.eq(cursor).and(playlist.id.lt(request.idAfter()))));
            return;
        }

        int cursor = parseIntegerCursor(request.cursor());
        where.and(ascending
                ? playlist.subscriberCount.gt(cursor)
                .or(playlist.subscriberCount.eq(cursor).and(playlist.id.gt(request.idAfter())))
                : playlist.subscriberCount.lt(cursor)
                .or(playlist.subscriberCount.eq(cursor).and(playlist.id.lt(request.idAfter()))));
    }

    private String normalizeSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank() || sortBy.equals("updatedAt")) {
            return "updatedAt";
        }
        if (sortBy.equals("subscribeCount")) {
            return "subscribeCount";
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }

    private String normalizeSortDirection(String sortDirection) {
        String value = sortDirection == null
                ? "DESCENDING"
                : sortDirection.toUpperCase(Locale.ROOT);

        if (!value.equals("ASCENDING") && !value.equals("DESCENDING")) {
            throw new BusinessException(ErrorCode.INVALID_SORT_DIRECTION);
        }
        return value;
    }

    private Instant parseInstantCursor(String cursor) {
        try {
            return Instant.parse(cursor);
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.INVALID_CURSOR);
        }
    }

    private int parseIntegerCursor(String cursor) {
        try {
            return Integer.parseInt(cursor);
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.INVALID_CURSOR);
        }
    }
}