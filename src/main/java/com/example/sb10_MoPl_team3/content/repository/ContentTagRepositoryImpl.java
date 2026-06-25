package com.example.sb10_MoPl_team3.content.repository;

import static com.example.sb10_MoPl_team3.content.entity.QContentTag.contentTag;
import static com.example.sb10_MoPl_team3.content.entity.QTag.tag;

import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ContentTagRepositoryImpl implements ContentTagRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<String> findTagNamesByContentId(UUID contentId) {
    return queryFactory
        .select(tag.name)
        .from(contentTag)
        .join(tag).on(contentTag.tag.id.eq(tag.id))
        .where(contentTag.id.contentId.eq(contentId))
        .fetch();
  }
}