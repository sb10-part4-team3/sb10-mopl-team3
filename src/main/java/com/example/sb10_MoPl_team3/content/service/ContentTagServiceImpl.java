package com.example.sb10_MoPl_team3.content.service;

import com.example.sb10_MoPl_team3.content.entity.ContentTag;
import com.example.sb10_MoPl_team3.content.entity.Tag;
import com.example.sb10_MoPl_team3.content.repository.ContentTagRepository;
import com.example.sb10_MoPl_team3.content.repository.TagRepository;
import java.util.ArrayList;
import java.util.List;
import com.example.sb10_MoPl_team3.content.entity.Content;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ContentTagServiceImpl implements ContentTagService {

  private final TagRepository tagRepository;
  private final ContentTagRepository contentTagRepository;
  // 태그 생성 충돌(unique 제약 위반)을 이 트랜잭션 안에서 catch하면, RDB 특성상 그 순간
  // 트랜잭션 전체가 aborted 상태가 되어 이후 같은 트랜잭션에서의 조회조차 실패한다.
  // REQUIRES_NEW로 별도 트랜잭션에서 시도해 충돌이 나도 바깥(호출자) 트랜잭션은 오염되지 않게 한다.
  private final TransactionTemplate requiresNewTransactionTemplate;

  public ContentTagServiceImpl(
      TagRepository tagRepository,
      ContentTagRepository contentTagRepository,
      PlatformTransactionManager transactionManager) {
    this.tagRepository = tagRepository;
    this.contentTagRepository = contentTagRepository;
    this.requiresNewTransactionTemplate = new TransactionTemplate(transactionManager);
    this.requiresNewTransactionTemplate.setPropagationBehavior(
        TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  @Override
  public List<String> syncTags(Content content, List<String> tagNames) {
    List<String> safeTagNames = (tagNames != null)
        ? tagNames.stream().distinct().toList()
        : List.of();

    contentTagRepository.deleteAllByContentId(content.getId());
    List<Tag> tags = new ArrayList<>();

    for (String name : safeTagNames) {
      tags.add(getOrCreateTag(name));
    }

    List<ContentTag> contentTags = new ArrayList<>();
    for (Tag tag : tags) {
      contentTags.add(new ContentTag(content, tag));
    }
    contentTagRepository.saveAll(contentTags);

    return tags.stream().map(Tag::getName).toList();
  }

  private Tag getOrCreateTag(String name) {
    return tagRepository.findByName(name)
        .orElseGet(() -> {
          try {
            return requiresNewTransactionTemplate.execute(
                status -> tagRepository.save(Tag.builder().name(name).build()));
          } catch (DataIntegrityViolationException e) {
            // 별도 트랜잭션에서만 실패했으므로 호출자의 트랜잭션은 멀쩡하다 — 안전하게 재조회 가능.
            return tagRepository.findByName(name).orElseThrow(() -> e);
          }
        });
  }
}
