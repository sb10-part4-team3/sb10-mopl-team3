package com.example.sb10_MoPl_team3.conversation.repository;

import static com.example.sb10_MoPl_team3.conversation.entity.QConversation.conversation;

import com.example.sb10_MoPl_team3.conversation.entity.Conversation;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

@RequiredArgsConstructor
public class ConversationRepositoryCustomImpl implements ConversationRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<Conversation> findParticipatingConversationsAsc(
      UUID userId,
      String keyword,
      Instant cursor,
      UUID idAfter,
      Pageable pageable
  ) {
    return queryFactory
        .selectFrom(conversation)
        .join(conversation.user1).fetchJoin()
        .join(conversation.user2).fetchJoin()
        .where(
            participatingCondition(userId),
            keywordCondition(userId, keyword),
            cursorAfterCondition(cursor, idAfter)
        )
        .orderBy(conversation.createdAt.asc(), conversation.id.asc())
        .limit(pageable.getPageSize())
        .fetch();
  }

  @Override
  public List<Conversation> findParticipatingConversationsDesc(
      UUID userId,
      String keyword,
      Instant cursor,
      UUID idAfter,
      Pageable pageable
  ) {
    return queryFactory
        .selectFrom(conversation)
        .join(conversation.user1).fetchJoin()
        .join(conversation.user2).fetchJoin()
        .where(
            participatingCondition(userId),
            keywordCondition(userId, keyword),
            cursorBeforeCondition(cursor, idAfter)
        )
        .orderBy(conversation.createdAt.desc(), conversation.id.desc())
        .limit(pageable.getPageSize())
        .fetch();
  }

  @Override
  public long countParticipatingConversations(UUID userId, String keyword) {
    Long count = queryFactory
        .select(conversation.count())
        .from(conversation)
        .where(
            participatingCondition(userId),
            keywordCondition(userId, keyword)
        )
        .fetchOne();

    return count == null ? 0L : count;
  }

  private BooleanExpression participatingCondition(UUID userId) {
    return conversation.user1.id.eq(userId)
        .or(conversation.user2.id.eq(userId));
  }

  private BooleanBuilder keywordCondition(UUID userId, String keyword) {
    BooleanBuilder builder = new BooleanBuilder();
    if (keyword == null || keyword.isBlank()) {
      return builder;
    }

    builder.and(
        conversation.user1.id.eq(userId)
            .and(
                conversation.user2.name.containsIgnoreCase(keyword)
                    .or(conversation.user2.email.containsIgnoreCase(keyword))
            )
            .or(
                conversation.user2.id.eq(userId)
                    .and(
                        conversation.user1.name.containsIgnoreCase(keyword)
                            .or(conversation.user1.email.containsIgnoreCase(keyword))
                    )
            )
    );
    return builder;
  }

  private BooleanExpression cursorAfterCondition(Instant cursor, UUID idAfter) {
    if (cursor == null) {
      return null;
    }

    return conversation.createdAt.gt(cursor)
        .or(conversation.createdAt.eq(cursor).and(conversation.id.gt(idAfter)));
  }

  private BooleanExpression cursorBeforeCondition(Instant cursor, UUID idAfter) {
    if (cursor == null) {
      return null;
    }

    return conversation.createdAt.lt(cursor)
        .or(conversation.createdAt.eq(cursor).and(conversation.id.lt(idAfter)));
  }
}
