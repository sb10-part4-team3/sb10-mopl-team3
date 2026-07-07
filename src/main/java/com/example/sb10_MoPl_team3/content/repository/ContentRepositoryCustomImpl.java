package com.example.sb10_MoPl_team3.content.repository;

import static com.example.sb10_MoPl_team3.content.entity.QContent.content;
import static com.example.sb10_MoPl_team3.content.entity.QContentStats.contentStats;
import static com.example.sb10_MoPl_team3.content.entity.QContentTag.contentTag;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.global.cursor.Cursor;
import com.example.sb10_MoPl_team3.global.cursor.CursorPageRequest;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTimeExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ContentRepositoryCustomImpl implements ContentRepositoryCustom {

  private static final String DEFAULT_SORT_BY = "createdAt";
  private final JPAQueryFactory queryFactory;

  private String normalizeSortBy(String sortBy) {
    return sortBy == null || sortBy.isBlank() ? DEFAULT_SORT_BY : sortBy;
  }


  @Override
  public List<Content> findContentsByCursor(
      CursorPageRequest pageRequest,
      ContentType typeEqual,
      String keywordLike,
      List<String> tagsIn
  ) {
    BooleanBuilder builder = buildBaseCondition(typeEqual, keywordLike, tagsIn);

    // 정렬 기준
    String sortBy = normalizeSortBy(pageRequest.sortBy());
    OrderSpecifier<?> orderSpecifier = switch (sortBy) {
      case "watcherCount" -> pageRequest.isAscending()
          ? contentStats.viewerCount.asc().nullsLast()
          : contentStats.viewerCount.desc().nullsLast();
      case "rate" -> pageRequest.isAscending()
          ? contentStats.averageRating.asc().nullsLast()
          : contentStats.averageRating.desc().nullsLast();
      default -> pageRequest.isAscending()
          ? sortDate().asc()
          : sortDate().desc();
    };

    // 커서 조건 (첫 페이지가 아닐 때만)
    if (pageRequest.hasCursor()) {
      if (pageRequest.idAfter() == null) {
        throw new IllegalArgumentException("idAfter is required when cursor is provided");
      }
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
   * 커서 기반 페이지네이션의 "다음 페이지" 조건을 만든다. 정렬 기준 컬럼이 커서값보다 크면(혹은 작으면) 통과, 같으면 id로 동점을 깨서 다음 항목을 가려낸다.
   */
  private BooleanExpression buildCursorCondition(CursorPageRequest pageRequest) {
    boolean ascending = pageRequest.isAscending();
    String sortBy = normalizeSortBy(pageRequest.sortBy());

    return switch (sortBy) {
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
            ? sortDate().gt(sortValue)
            : sortDate().lt(sortValue);
        BooleanExpression tieBreak = sortDate().eq(sortValue)
            .and(content.id.gt(cursor.id()));
        yield compare.or(tieBreak);
      }
    };
  }

  /**
   * "최신순" 정렬 기준. 스포츠처럼 실제 사건 일자(eventDate)가 있으면 그 값을, 없으면(영화/TV 등)
   * DB 저장 시각(createdAt)을 기준으로 삼는다.
   */
  private DateTimeExpression<Instant> sortDate() {
    return content.eventDate.coalesce(content.createdAt);
  }

  @Override
  public long countContents(ContentType typeEqual, String keywordLike, List<String> tagsIn) {
    BooleanBuilder builder = buildBaseCondition(typeEqual, keywordLike, tagsIn);

    Long count = queryFactory
        .select(content.count())
        .from(content)
        .where(builder)
        .fetchOne();

    return count != null ? count : 0L;
  }

  private BooleanBuilder buildBaseCondition(ContentType typeEqual, String keywordLike,
      List<String> tagsIn) {
    BooleanBuilder builder = new BooleanBuilder();

    if (typeEqual != null) {
      builder.and(content.type.eq(typeEqual));
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

    return builder;

  }
}