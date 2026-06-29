package com.example.sb10_MoPl_team3.content.repository;

import static com.example.sb10_MoPl_team3.content.entity.QContent.content;
import static com.example.sb10_MoPl_team3.content.entity.QContentStats.contentStats;
import static com.example.sb10_MoPl_team3.content.entity.QContentTag.contentTag;

import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.global.cursor.Cursor;
import com.example.sb10_MoPl_team3.global.cursor.CursorPageRequest;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ContentRepositoryCustomImpl implements ContentRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<Content> findContentsByCursor(
      CursorPageRequest pageRequest,
      String typeEqual,
      String keywordLike,
      List<String> tagsIn
  ) {
    BooleanBuilder builder = new BooleanBuilder();

    if (typeEqual != null) {
      builder.and(content.type.stringValue().eq(typeEqual));
    }
    if (keywordLike != null && !keywordLike.isBlank()) {
      builder.and(content.title.containsIgnoreCase(keywordLike));
    }
    if (tagsIn != null && !tagsIn.isEmpty()) {
      builder.and(
          content.id.in(
              JPAExpressions
                  .select(contentTag.content.id)
                  .from(contentTag)
                  .where(contentTag.tag.name.in(tagsIn))
          )
      );
    }

    // 정렬 기준
    OrderSpecifier<?> orderSpecifier = switch (pageRequest.sortBy()) {
      case "watcherCount" -> pageRequest.isAscending()
          ? contentStats.viewerCount.asc()
          : contentStats.viewerCount.desc();
      case "rate" -> pageRequest.isAscending()
          ? contentStats.averageRating.asc()
          : contentStats.averageRating.desc();
      default -> pageRequest.isAscending()
          ? content.createdAt.asc()
          : content.createdAt.desc();
    };

    // 커서 조건 (첫 페이지가 아닐 때만)
    if (pageRequest.hasCursor()) {
      builder.and(buildCursorCondition(pageRequest));
    }

    return queryFactory
        .selectFrom(content)
        .leftJoin(contentStats).on(contentStats.content.eq(content))
        .where(builder)
        .orderBy(orderSpecifier, content.id.asc())
        .limit(pageRequest.size() + 1L)
        .fetch();
  }

  /**
   * 커서 기반 페이지네이션의 "다음 페이지" 조건을 만든다.
   * 정렬 기준 컬럼이 커서값보다 크면(혹은 작으면) 통과,
   * 같으면 id로 동점을 깨서 다음 항목을 가려낸다.
   */
  private BooleanExpression buildCursorCondition(CursorPageRequest pageRequest) {
    boolean ascending = pageRequest.isAscending();

    return switch (pageRequest.sortBy()) {
      case "watcherCount" -> {
        Cursor<Integer> cursor = Cursor.from(pageRequest, Integer::valueOf);
        Integer sortValue = cursor.sortValue();
        BooleanExpression compare = ascending
            ? contentStats.viewerCount.gt(sortValue)
            : contentStats.viewerCount.lt(sortValue);
        BooleanExpression tieBreak = contentStats.viewerCount.eq(sortValue)
            .and(content.id.gt(cursor.id()));
        yield compare.or(tieBreak);
      }
      case "rate" -> {
        Cursor<BigDecimal> cursor = Cursor.from(pageRequest, BigDecimal::new);
        BigDecimal sortValue = cursor.sortValue();
        BooleanExpression compare = ascending
            ? contentStats.averageRating.gt(sortValue)
            : contentStats.averageRating.lt(sortValue);
        BooleanExpression tieBreak = contentStats.averageRating.eq(sortValue)
            .and(content.id.gt(cursor.id()));
        yield compare.or(tieBreak);
      }
      default -> {
        Cursor<Instant> cursor = Cursor.from(pageRequest, Instant::parse);
        Instant sortValue = cursor.sortValue();
        BooleanExpression compare = ascending
            ? content.createdAt.gt(sortValue)
            : content.createdAt.lt(sortValue);
        BooleanExpression tieBreak = content.createdAt.eq(sortValue)
            .and(content.id.gt(cursor.id()));
        yield compare.or(tieBreak);
      }
    };
  }

  @Override
  public long countContents(String typeEqual, String keywordLike, List<String> tagsIn) {
    BooleanBuilder builder = new BooleanBuilder();

    if (typeEqual != null) {
      builder.and(content.type.stringValue().eq(typeEqual));
    }
    if (keywordLike != null && !keywordLike.isBlank()) {
      builder.and(content.title.containsIgnoreCase(keywordLike));
    }
    if (tagsIn != null && !tagsIn.isEmpty()) {
      builder.and(
          content.id.in(
              JPAExpressions
                  .select(contentTag.content.id)
                  .from(contentTag)
                  .where(contentTag.tag.name.in(tagsIn))
          )
      );
    }

    Long count = queryFactory
        .select(content.count())
        .from(content)
        .where(builder)
        .fetchOne();

    return count != null ? count : 0L;
  }
}